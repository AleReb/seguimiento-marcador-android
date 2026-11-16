using System;
using System.Net;
using System.Net.Sockets;
using System.Text;
using System.Threading;
using UnityEngine;

// Añadir a Assets de un proyecto Unity. Construye la escena al iniciar Play.
public sealed class MarcadorDemo : MonoBehaviour
{
    [Serializable] private sealed class Pose
    {
        public int version;
        public float cubeSizeCm;
        public bool tracked;
        public int id;
        public float x, y, z;
        public float[] rotation;
    }

    private readonly object sync = new object();
    private string pending;
    private UdpClient receiver;
    private Thread thread;
    private volatile bool running;
    private GameObject cube;
    private Vector3 origin;
    private Quaternion originRotation;
    private bool hasOrigin;
    private int markerId = -1;
    private float lastPoseTime;
    [Header("Demo de profundidad V2")]
    [Range(1f, 40f)] public float depthGain = 12f;
    [Range(1f, 10f)] public float lateralGain = 3f;
    public bool followDepth = true;
    private Camera demoCamera;
    private Vector3 targetPosition;
    private Quaternion targetRotation = Quaternion.identity;
    private float measuredDepthCm;
    private bool resetRequested;
    private Material guideMaterial;

    [RuntimeInitializeOnLoadMethod(RuntimeInitializeLoadType.AfterSceneLoad)]
    private static void Bootstrap()
    {
        if (FindFirstObjectByType<MarcadorDemo>() == null)
            new GameObject("Receptor de marcador").AddComponent<MarcadorDemo>();
    }

    private void Start()
    {
        var cam = Camera.main;
        if (cam == null) { var obj = new GameObject("Main Camera"); obj.tag = "MainCamera"; cam = obj.AddComponent<Camera>(); }
        demoCamera = cam;
        cam.nearClipPlane = 0.05f;
        cam.farClipPlane = 500f;
        cam.fieldOfView = 55f;
        cam.transform.position = new Vector3(7, 5, -14);
        cam.transform.LookAt(Vector3.zero);
        cam.backgroundColor = new Color(.05f, .11f, .15f);
        cam.clearFlags = CameraClearFlags.SolidColor;
        var lightObj = new GameObject("Luz");
        var lightComponent = lightObj.AddComponent<Light>();
        lightComponent.type = LightType.Directional;
        lightObj.transform.rotation = Quaternion.Euler(45, -30, 0);
        cube = GameObject.CreatePrimitive(PrimitiveType.Cube);
        cube.name = "Marcador 3D";
        cube.transform.localScale = Vector3.one * 0.7f;
        cube.GetComponent<Renderer>().material.color = new Color(.2f, .85f, .65f);
        // Mostrar el cubo inicial incluso antes de recibir datos.
        cube.SetActive(true);
        BuildDepthGuides();
        gameObject.AddComponent<DepthDemoPanel>().Initialize(this);
        try {
            receiver = new UdpClient(5005);
            running = true;
            thread = new Thread(Receive) { IsBackground = true };
            thread.Start();
            Debug.Log("Esperando marcador por UDP en puerto 5005. Pulsa R para establecer un nuevo origen.");
        } catch (Exception e) { Debug.LogError("No se pudo abrir el puerto 5005: " + e.Message); }
    }

    private void Receive()
    {
        var endpoint = new IPEndPoint(IPAddress.Any, 0);
        while (running) {
            try {
                byte[] bytes = receiver.Receive(ref endpoint);
                if (bytes.Length > 2048) continue;
                lock (sync) pending = Encoding.UTF8.GetString(bytes);
            } catch (SocketException) { if (!running) break; }
              catch (ObjectDisposedException) { break; }
        }
    }

    private void Update()
    {
        ReadDepthControls();
#if ENABLE_LEGACY_INPUT_MANAGER
        if (Input.GetKeyDown(KeyCode.R)) resetRequested = true;
#endif
        if (resetRequested) {
            hasOrigin = false; markerId = -1; targetPosition = Vector3.zero;
            cube.transform.position = Vector3.zero;
            resetRequested = false;
        }
        // Teclado como alternativa hasta recibir datos del marcador.
        if (!hasOrigin) SimularConTeclado();
        string message;
        lock (sync) { message = pending; pending = null; }
        if (message != null) {
            try {
                var pose = JsonUtility.FromJson<Pose>(message);
                if (pose != null && pose.version == 1 && pose.tracked && pose.rotation != null && pose.rotation.Length == 9 &&
                    Finite(pose.x) && Finite(pose.y) && Finite(pose.z) && Array.TrueForAll(pose.rotation, Finite)) {
                    if (Finite(pose.cubeSizeCm) && pose.cubeSizeCm >= 1f && pose.cubeSizeCm <= 20f)
                        cube.transform.localScale = Vector3.one * (0.7f * pose.cubeSizeCm / 5f);
                    var r = pose.rotation;
                    var position = new Vector3(pose.x, -pose.y, pose.z) * 0.01f;
                    // Cambio de base de OpenCV (Y abajo) a Unity (Y arriba).
                    var forward = new Vector3(r[2], -r[5], r[8]);
                    var up = new Vector3(-r[1], r[4], -r[7]);
                    if (forward.sqrMagnitude < .1f || up.sqrMagnitude < .1f || Vector3.Cross(forward, up).sqrMagnitude < .01f) return;
                    var rotation = Quaternion.LookRotation(forward, up);
                    if (!hasOrigin || markerId != pose.id) { origin = position; originRotation = rotation; markerId = pose.id; hasOrigin = true; }
                    var delta = position - origin;
                    measuredDepthCm = delta.z * 100f;
                    targetPosition = new Vector3(delta.x * lateralGain, delta.y * lateralGain, delta.z * depthGain);
                    targetRotation = rotation * Quaternion.Inverse(originRotation);
                    cube.SetActive(true);
                    lastPoseTime = Time.unscaledTime;
                } else if (pose != null && !pose.tracked) cube.SetActive(false);
            } catch (Exception) { /* Ignorar paquetes dañados. */ }
        }
        if (hasOrigin && cube.activeSelf && Time.unscaledTime - lastPoseTime > 0.5f) cube.SetActive(false);
        if (hasOrigin && cube.activeSelf) {
            float blend = 1f - Mathf.Exp(-12f * Time.unscaledDeltaTime);
            cube.transform.position = Vector3.Lerp(cube.transform.position, targetPosition, blend);
            cube.transform.rotation = Quaternion.Slerp(cube.transform.rotation, targetRotation, blend);
        }
        if (demoCamera != null) {
            float z = followDepth ? cube.transform.position.z : 0f;
            // Follow more closely on approach, keeping the camera behind the cube.
            float follow = z < 0 ? z : z * 0.35f;
            demoCamera.transform.position = Vector3.Lerp(demoCamera.transform.position,
                new Vector3(7, 5, -14 + follow), 1f - Mathf.Exp(-5f * Time.unscaledDeltaTime));
            demoCamera.transform.LookAt(new Vector3(0, 0, follow + 2));
        }
    }

    private void SimularConTeclado()
    {
        float horizontal = 0f, vertical = 0f, depth = 0f, turn = 0f;
#if ENABLE_INPUT_SYSTEM
        var keyboard = UnityEngine.InputSystem.Keyboard.current;
        if (keyboard == null) return;
        horizontal = (keyboard.rightArrowKey.isPressed ? 1f : 0f) - (keyboard.leftArrowKey.isPressed ? 1f : 0f);
        vertical = (keyboard.upArrowKey.isPressed ? 1f : 0f) - (keyboard.downArrowKey.isPressed ? 1f : 0f);
        depth = (keyboard.wKey.isPressed ? 1f : 0f) - (keyboard.sKey.isPressed ? 1f : 0f);
        turn = (keyboard.eKey.isPressed ? 1f : 0f) - (keyboard.qKey.isPressed ? 1f : 0f);
#elif ENABLE_LEGACY_INPUT_MANAGER
        horizontal = (Input.GetKey(KeyCode.RightArrow) ? 1f : 0f) - (Input.GetKey(KeyCode.LeftArrow) ? 1f : 0f);
        vertical = (Input.GetKey(KeyCode.UpArrow) ? 1f : 0f) - (Input.GetKey(KeyCode.DownArrow) ? 1f : 0f);
        depth = (Input.GetKey(KeyCode.W) ? 1f : 0f) - (Input.GetKey(KeyCode.S) ? 1f : 0f);
        turn = (Input.GetKey(KeyCode.E) ? 1f : 0f) - (Input.GetKey(KeyCode.Q) ? 1f : 0f);
#endif
        cube.transform.position += new Vector3(horizontal * 2f, vertical * 2f, depth * 8f) * Time.deltaTime;
        cube.transform.Rotate(Vector3.up, turn * 90f * Time.deltaTime, Space.World);
    }

    public float MeasuredDepthCm => measuredDepthCm;
    public float SceneDepth => cube == null ? 0f : cube.transform.position.z;
    public string ConnectionStatus => !running ? "No se pudo abrir UDP 5005" :
        !hasOrigin ? "UDP 5005 · esperando marcador" :
        Time.unscaledTime - lastPoseTime > 0.5f || !cube.activeSelf ? "UDP 5005 · señal perdida" :
        "UDP 5005 · recibiendo ID " + markerId;
    public void Recenter() { resetRequested = true; }
    public void ChangeDepthGain(float amount)
    {
        depthGain = Mathf.Clamp(depthGain + amount, 1f, 40f);
        if (hasOrigin) targetPosition.z = measuredDepthCm * 0.01f * depthGain;
    }

    private void ReadDepthControls()
    {
        bool increase = false, decrease = false;
#if ENABLE_INPUT_SYSTEM
        var keyboard = UnityEngine.InputSystem.Keyboard.current;
        if (keyboard != null) {
            if (keyboard.rKey.wasPressedThisFrame) resetRequested = true;
            if (keyboard.cKey.wasPressedThisFrame) followDepth = !followDepth;
            increase = keyboard.equalsKey.wasPressedThisFrame || keyboard.numpadPlusKey.wasPressedThisFrame;
            decrease = keyboard.minusKey.wasPressedThisFrame || keyboard.numpadMinusKey.wasPressedThisFrame;
        }
#elif ENABLE_LEGACY_INPUT_MANAGER
        if (Input.GetKeyDown(KeyCode.C)) followDepth = !followDepth;
        increase = Input.GetKeyDown(KeyCode.Equals) || Input.GetKeyDown(KeyCode.KeypadPlus);
        decrease = Input.GetKeyDown(KeyCode.Minus) || Input.GetKeyDown(KeyCode.KeypadMinus);
#endif
        if (increase || decrease) {
            ChangeDepthGain(increase ? 1f : -1f);
            Debug.Log("Profundidad V2: ganancia Z = " + depthGain + ". R para centrar; C cambia seguimiento de cámara.");
        }
    }

    private void BuildDepthGuides()
    {
        var shader = Shader.Find("Sprites/Default");
        if (shader == null) return;
        guideMaterial = new Material(shader);
        // Distance stripes are scene units, not physical measurements.
        for (int z = -60; z <= 120; z += 2) {
            var line = new GameObject("Referencia Z " + z).AddComponent<LineRenderer>();
            line.transform.SetParent(transform);
            line.sharedMaterial = guideMaterial;
            line.positionCount = 2; line.useWorldSpace = true;
            line.SetPosition(0, new Vector3(-5, -2, z));
            line.SetPosition(1, new Vector3(5, -2, z));
            line.startWidth = line.endWidth = z == 0 ? 0.07f : 0.025f;
            var color = z == 0 ? Color.yellow : new Color(0.15f, 0.38f, 0.48f, 0.7f);
            line.startColor = line.endColor = color;
        }
        for (int x = -5; x <= 5; x += 2) {
            var line = new GameObject("Guia longitudinal").AddComponent<LineRenderer>();
            line.transform.SetParent(transform); line.sharedMaterial = guideMaterial;
            line.positionCount = 2; line.startWidth = line.endWidth = 0.025f;
            line.SetPosition(0, new Vector3(x, -2, -60)); line.SetPosition(1, new Vector3(x, -2, 120));
            line.startColor = line.endColor = new Color(0.15f, 0.38f, 0.48f, 0.7f);
        }
    }

    private static bool Finite(float value) => !float.IsNaN(value) && !float.IsInfinity(value);
    private void OnDestroy() { running = false; receiver?.Close(); if (thread != null && thread.IsAlive) thread.Join(500); if (guideMaterial != null) Destroy(guideMaterial); }
}
