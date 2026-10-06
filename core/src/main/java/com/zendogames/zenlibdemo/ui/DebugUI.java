package com.zendogames.zenlibdemo.ui;

import com.badlogic.gdx.Gdx;
import com.github.xpenatan.jparser.runtime.helper.NativeBoolArray;
import imgui.ImGui;
import imgui.ImTemp;
import imgui.enums.ImGuiCond;

public class DebugUI {

    public void render() {
        ImGui.SetNextWindowCollapsed(true, ImGuiCond.Once);
        ImGui.SetNextWindowPos(ImTemp.ImVec2_1(0, 0), ImGuiCond.Once);
        ImGui.SetNextWindowSize(ImTemp.ImVec2_1(Gdx.graphics.getWidth(), Gdx.graphics.getHeight() / 5f), ImGuiCond.Once);
        ImGui.Begin("Debug");
        {
            // ---- ImGui Tool toggles ----
            ImGui.SeparatorText("ImGui Tools");
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
        ImGui.End();
    }

    private static class ImGuiTools {
        static final NativeBoolArray demo     = new NativeBoolArray(1);
        static final NativeBoolArray metrics  = new NativeBoolArray(1);
        static final NativeBoolArray debugLog = new NativeBoolArray(1);
        static final NativeBoolArray idStack  = new NativeBoolArray(1);
        static final NativeBoolArray about    = new NativeBoolArray(1);
        static final NativeBoolArray styles   = new NativeBoolArray(1);
    }
}
