package com.zendogames.zenlibdemo.ui;

import com.badlogic.gdx.Gdx;
import com.github.xpenatan.jparser.runtime.helper.NativeBoolArray;
import com.zendogames.zenlib.ui.DebugActions;
import com.zendogames.zenlib.ui.DebugView;
import com.zendogames.zenlib.ui.DebugViews;
import imgui.ImGui;
import imgui.ImTemp;
import imgui.enums.ImGuiCond;
import imgui.enums.ImGuiTreeNodeFlags;

/**
 * The one debug window: a full-width strip at the top, collapsed on first show, with the ImGui
 * tool toggles, then one collapsing header per {@link DebugView} the current screen passes in.
 * Views come from {@link DebugViews} (library objects) and {@link DebugActions} (screen controls);
 * ImGui remembers each header's open state by title for the life of the process.
 */
public class DebugUI {

    private final DebugView framePool = DebugViews.framePool();

    /** Draws the Debug window with the frame pool view and {@code views}, in order. Call once per frame from {@code renderImGui}. */
    public void render(DebugView... views) {
        ImGui.SetNextWindowCollapsed(true, ImGuiCond.Once);
        ImGui.SetNextWindowPos(ImTemp.ImVec2_1(0, 0), ImGuiCond.Once);
        ImGui.SetNextWindowSize(ImTemp.ImVec2_1(Gdx.graphics.getWidth(), 0), ImGuiCond.Once);
        ImGui.Begin("Debug");
        {
            tools();
            for (var view : views) {
                section(view);
            }
            section(framePool);
        }
        ImGui.End();
    }

    private void section(DebugView view) {
        var flags = view.openByDefault() ? ImGuiTreeNodeFlags.DefaultOpen : ImGuiTreeNodeFlags.None;
        if (ImGui.CollapsingHeader(view.title(), flags)) {
            ImGui.Indent();
            view.draw();
            ImGui.Unindent();
        }
    }

    private void tools() {
        if (!ImGui.CollapsingHeader("ImGui Tools")) return;
                          ImGui.Checkbox("demo",      ImGuiTools.demo);
        ImGui.SameLine(); ImGui.Checkbox("metrics",   ImGuiTools.metrics);
        ImGui.SameLine(); ImGui.Checkbox("debug-log", ImGuiTools.debugLog);
        ImGui.SameLine(); ImGui.Checkbox("id-stack",  ImGuiTools.idStack);
        ImGui.SameLine(); ImGui.Checkbox("about",     ImGuiTools.about);
        ImGui.SameLine(); ImGui.Checkbox("styles",    ImGuiTools.styles);

        if (ImGuiTools.demo.getValue(0))     ImGui.ShowDemoWindow(ImGuiTools.demo);
        if (ImGuiTools.metrics.getValue(0))  ImGui.ShowMetricsWindow(ImGuiTools.metrics);
        if (ImGuiTools.debugLog.getValue(0)) ImGui.ShowDebugLogWindow(ImGuiTools.debugLog);
        if (ImGuiTools.idStack.getValue(0))  ImGui.ShowIDStackToolWindow(ImGuiTools.idStack);
        if (ImGuiTools.about.getValue(0))    ImGui.ShowAboutWindow(ImGuiTools.about);
        if (ImGuiTools.styles.getValue(0))   ImGui.ShowStyleEditor();
    }


    /** Native buffers allocated on first use, after {@code ImGuiGdx.load()}. */
    private static class ImGuiTools {
        static final NativeBoolArray demo     = new NativeBoolArray(1);
        static final NativeBoolArray metrics  = new NativeBoolArray(1);
        static final NativeBoolArray debugLog = new NativeBoolArray(1);
        static final NativeBoolArray idStack  = new NativeBoolArray(1);
        static final NativeBoolArray about    = new NativeBoolArray(1);
        static final NativeBoolArray styles   = new NativeBoolArray(1);
    }
}
