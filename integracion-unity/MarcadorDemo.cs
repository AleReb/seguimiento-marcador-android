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
    private const float Scale = 3f;

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
        cam.transform.position = new Vector3(0, 1, -7);
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
        cube.SetActive(false);
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
        if (Input.GetKeyDown(KeyCode.R)) hasOrigin = false;
        string message;
        lock (sync) { message = pending; pending = null; }
        if (message != null) {
            try {
                var pose = JsonUtility.FromJson<Pose>(message);
                if (pose != null && pose.version == 1 && pose.tracked && pose.rotation != null && pose.rotation.Length == 9 &&
                    Finite(pose.x) && Finite(pose.y) && Finite(pose.z) && Array.TrueForAll(pose.rotation, Finite)) {
                    var r = pose.rotation;
                    var position = new Vector3(pose.x, -pose.y, pose.z) * 0.01f;
                    // Cambio de base de OpenCV (Y abajo) a Unity (Y arriba).
                    var forward = new Vector3(r[2], -r[5], r[8]);
                    var up = new Vector3(-r[1], r[4], -r[7]);
                    if (forward.sqrMagnitude < .1f || up.sqrMagnitude < .1f || Vector3.Cross(forward, up).sqrMagnitude < .01f) return;
                    var rotation = Quaternion.LookRotation(forward, up);
                    if (!hasOrigin || markerId != pose.id) { origin = position; originRotation = rotation; markerId = pose.id; hasOrigin = true; }
                    cube.transform.position = (position - origin) * Scale;
                    cube.transform.rotation = rotation * Quaternion.Inverse(originRotation);
                    cube.SetActive(true);
                    lastPoseTime = Time.unscaledTime;
                } else if (pose != null && !pose.tracked) cube.SetActive(false);
            } catch (Exception) { /* Ignorar paquetes dañados. */ }
        }
        if (cube.activeSelf && Time.unscaledTime - lastPoseTime > 0.5f) cube.SetActive(false);
    }

    private static bool Finite(float value) => !float.IsNaN(value) && !float.IsInfinity(value);
    private void OnDestroy() { running = false; receiver?.Close(); if (thread != null && thread.IsAlive) thread.Join(500); }
}
