using UnityEngine;
using TMPro;

// Runtime uGUI panel. It only controls this demo; pose reception stays in MarcadorDemo.
public sealed class DepthDemoPanel : MonoBehaviour
{
    private MarcadorDemo demo;
    private TextMeshProUGUI status;
    private TMP_FontAsset font;

    public void Initialize(MarcadorDemo source)
    {
        demo = source;
        font = Resources.Load<TMP_FontAsset>("Fonts & Materials/LiberationSans SDF");
        var canvasObject = new GameObject("Panel Profundidad V2", typeof(RectTransform), typeof(Canvas),
            typeof(UnityEngine.UI.CanvasScaler), typeof(UnityEngine.UI.GraphicRaycaster));
        canvasObject.transform.SetParent(transform, false);
        canvasObject.GetComponent<Canvas>().renderMode = RenderMode.ScreenSpaceOverlay;
        var scaler = canvasObject.GetComponent<UnityEngine.UI.CanvasScaler>();
        scaler.uiScaleMode = UnityEngine.UI.CanvasScaler.ScaleMode.ScaleWithScreenSize;
        scaler.referenceResolution = new Vector2(1280, 720); scaler.matchWidthOrHeight = 0.5f;
        var panel = Rect("Profundidad", canvasObject.transform, 16, 16, 470, 272);
        var background = panel.gameObject.AddComponent<UnityEngine.UI.Image>();
        background.color = new Color(0.025f, 0.05f, 0.08f, 0.94f); background.raycastTarget = false;
        Label(panel, "CUBO · PROFUNDIDAD V2", 16, 12, 438, 30, 22);
        status = Label(panel, "Esperando UDP 5005…", 16, 48, 438, 104, 18);
        Button(panel, "− Z", 16, 160, 82, () => demo.ChangeDepthGain(-1));
        Button(panel, "+ Z", 106, 160, 82, () => demo.ChangeDepthGain(1));
        Button(panel, "Centrar", 196, 160, 110, demo.Recenter);
        Button(panel, "Cámara", 314, 160, 140, () => demo.followDepth = !demo.followDepth);
        Label(panel, "+ / −: ganancia   R: centrar   C: cámara\nSin señal: flechas, W/S (Z), Q/E (giro)", 16, 210, 438, 52, 16);
        if (FindFirstObjectByType<UnityEngine.EventSystems.EventSystem>() == null) {
            var events = new GameObject("EventSystem", typeof(UnityEngine.EventSystems.EventSystem));
#if ENABLE_INPUT_SYSTEM
            events.AddComponent<UnityEngine.InputSystem.UI.InputSystemUIInputModule>();
#else
            events.AddComponent<UnityEngine.EventSystems.StandaloneInputModule>();
#endif
        }
    }

    private void Update()
    {
        if (demo == null || status == null) return;
        var offset = demo.MeasuredOffsetCm;
        status.text = demo.ConnectionStatus + "\n" +
            $"Δ X {offset.x:+0.0;-0.0;0.0}  Y {offset.y:+0.0;-0.0;0.0}  Z {offset.z:+0.0;-0.0;0.0} cm\n" +
            $"Ganancia XY ×{demo.lateralGain:0}  ·  Z ×{demo.depthGain:0}  ·  Cámara: {(demo.followDepth ? "sigue Z" : "fija")}\n" +
            "<color=#FF5959>X rojo</color>  ·  <color=#73F273>Y verde</color>  ·  <color=#59B3FF>Z azul</color>";
    }

    private RectTransform Rect(string name, Transform parent, float x, float y, float w, float h)
    {
        var rect = new GameObject(name, typeof(RectTransform)).GetComponent<RectTransform>();
        rect.SetParent(parent, false); rect.anchorMin = rect.anchorMax = rect.pivot = new Vector2(0, 1);
        rect.anchoredPosition = new Vector2(x, -y); rect.sizeDelta = new Vector2(w, h); return rect;
    }
    private TextMeshProUGUI Label(Transform parent, string value, float x, float y, float w, float h, int size)
    {
        var text = Rect("Texto", parent, x, y, w, h).gameObject.AddComponent<TextMeshProUGUI>();
        if (font != null) text.font = font;
        text.text = value; text.fontSize = size; text.color = Color.white; text.raycastTarget = false;
        return text;
    }
    private void Button(Transform parent, string title, float x, float y, float w, UnityEngine.Events.UnityAction action)
    {
        var rect = Rect(title, parent, x, y, w, 40);
        var image = rect.gameObject.AddComponent<UnityEngine.UI.Image>(); image.color = new Color(0.08f, 0.33f, 0.4f);
        var button = rect.gameObject.AddComponent<UnityEngine.UI.Button>(); button.targetGraphic = image;
        button.onClick.AddListener(action);
        var text = Label(rect, title, 0, 0, w, 40, 19); text.alignment = TextAlignmentOptions.Center;
    }
}
