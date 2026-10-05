package com.zendogames.zenlibdemo;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.ScreenUtils;
import com.github.xpenatan.jparser.runtime.helper.NativeBoolArray;
import com.zendogames.zenlib.ZenFlag;
import com.zendogames.zenlib.ZenGame;
import com.zendogames.zenlib.imgui.ImGuiGdx;
import com.zendogames.zenlib.screens.ZenScreen;
import imgui.ImGui;
import imgui.ImTemp;
import imgui.enums.ImGuiCond;

/** First screen of the application. Displayed after the application is created. */
public class FirstScreen extends ZenScreen {

    TextureRegion region;

    public FirstScreen(ZenGame game) {
        super(game);
    }

    @Override
    public void show() {
        region = new TextureRegion(game.pixel);
        if (ImGuiGdx.isInitialized()) {
            ZenFlag.SHOW_IMGUI.enable();
        }
    }

    @Override
    public void render(float delta) {
        var margin = 50;
        var w = Gdx.graphics.getWidth();
        var h = Gdx.graphics.getHeight();

        ScreenUtils.clear(Color.LIGHT_GRAY);
        batch.begin();
        batch.setColor(Color.FOREST);
        batch.draw(region, margin, margin, w - 2*margin, h - 2*margin);
        batch.setColor(Color.WHITE);
        batch.end();
    }

    @Override
    public void renderImGui() {
        // TODO: move to a DebugUI class
        ImGui.SetNextWindowCollapsed(true, ImGuiCond.Once);
        ImGui.SetNextWindowPos(ImTemp.ImVec2_1(0, 0), ImGuiCond.Once);
        ImGui.SetNextWindowSize(ImTemp.ImVec2_1(Gdx.graphics.getWidth(), Gdx.graphics.getHeight() / 4f), ImGuiCond.Once);
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

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);

        // If the window is minimized on a desktop (LWJGL3) platform, width and height are 0, which causes problems.
        // In that case, we don't resize anything, and wait for the window to be a normal size before updating.
        if(width <= 0 || height <= 0) return;

        // Resize your screen here. The parameters represent the new window size.
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