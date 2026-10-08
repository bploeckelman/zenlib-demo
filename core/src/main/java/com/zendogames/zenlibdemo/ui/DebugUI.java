package com.zendogames.zenlibdemo.ui;

import com.badlogic.gdx.Gdx;
import com.github.xpenatan.jparser.runtime.helper.NativeBoolArray;
import com.zendogames.zenlib.FramePool;
import com.zendogames.zenlib.ecs.World;
import imgui.ImGui;
import imgui.ImTemp;
import imgui.enums.ImGuiCond;
import imgui.enums.ImGuiTableColumnFlags;
import imgui.enums.ImGuiTableFlags;
import imgui.enums.ImGuiWindowFlags;

public class DebugUI {

    public void render() {
        ImGui.SetNextWindowCollapsed(true, ImGuiCond.Once);
        ImGui.SetNextWindowPos(ImTemp.ImVec2_1(0, 0), ImGuiCond.Once);
        ImGui.SetNextWindowSize(ImTemp.ImVec2_1(Gdx.graphics.getWidth(), 0), ImGuiCond.Once);
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

            // ---- Frame Pool usage ----
            ImGui.SeparatorText("FramePool Usage");
            var tableFlags = ImGuiTableFlags.Borders.or(ImGuiTableFlags.RowBg).or(ImGuiTableFlags.SizingStretchSame);
            if (ImGui.BeginTable("frame-pool", 4, tableFlags)) {
                ImGui.TableSetupColumn("Type",   ImGuiTableColumnFlags.WidthStretch);
                ImGui.TableSetupColumn("Active", ImGuiTableColumnFlags.WidthStretch);
                ImGui.TableSetupColumn("Peak",   ImGuiTableColumnFlags.WidthStretch);
                ImGui.TableSetupColumn("Free",   ImGuiTableColumnFlags.WidthStretch);

                ImGui.TableHeadersRow();
                for (var type : FramePool.types()) {
                    ImGui.TableNextRow();
                    ImGui.TableNextColumn(); ImGui.TextUnformatted(type.name());
                    ImGui.TableNextColumn(); ImGui.TextUnformatted(String.valueOf(FramePool.active(type)));
                    ImGui.TableNextColumn(); ImGui.TextUnformatted(String.valueOf(FramePool.peak(type)));
                    ImGui.TableNextColumn(); ImGui.TextUnformatted(String.valueOf(FramePool.free(type)));
                }
                ImGui.EndTable();
            }
        }
        ImGui.End();
    }

    /** Entity count, interpolation alpha, and a table of systems with live enabled toggles. */
    public void ecs(World world) {
        if (!EcsFlag.open.getValue(0)) return;

        ImGui.SetNextWindowPos(ImTemp.ImVec2_1(0, 40), ImGuiCond.Once);
        // NoFocusOnAppearing: a newly shown window would take focus, and the pinned ImGuiGdxInput
        // swallows every key while any window is focused, so the screen's keys would go dead
        // until a click outside it. The bindings need a non-null p_open, hence EcsFlag.open.
        if (ImGui.Begin("ECS", EcsFlag.open, ImGuiWindowFlags.NoFocusOnAppearing)) {
            ImGui.TextUnformatted("entities: " + world.entities().size);
            ImGui.TextUnformatted("alpha: %.3f".formatted(world.alpha()));
            ImGui.TextUnformatted("updating: " + world.isUpdating());

            var tableFlags = ImGuiTableFlags.Borders.or(ImGuiTableFlags.RowBg).or(ImGuiTableFlags.SizingStretchSame);
            if (ImGui.BeginTable("systems", 4, tableFlags)) {
                ImGui.TableSetupColumn("System",   ImGuiTableColumnFlags.WidthStretch);
                ImGui.TableSetupColumn("Priority", ImGuiTableColumnFlags.WidthStretch);
                ImGui.TableSetupColumn("Runs",     ImGuiTableColumnFlags.WidthStretch);
                ImGui.TableSetupColumn("Enabled",  ImGuiTableColumnFlags.WidthStretch);

                ImGui.TableHeadersRow();
                var systems = world.systems();
                for (int i = 0; i < systems.size; i++) {
                    var system = systems.get(i);
                    ImGui.PushID(i);
                    ImGui.TableNextRow();
                    ImGui.TableNextColumn(); ImGui.TextUnformatted(system.getClass().getSimpleName());
                    ImGui.TableNextColumn(); ImGui.TextUnformatted(String.valueOf(system.priority));
                    ImGui.TableNextColumn(); ImGui.TextUnformatted(system.runsOnTick() ? "tick" : "render");
                    ImGui.TableNextColumn();
                    EcsFlag.systemEnabled.setValue(0, system.enabled);
                    if (ImGui.Checkbox("##enabled", EcsFlag.systemEnabled)) {
                        system.enabled = EcsFlag.systemEnabled.getValue(0);
                    }
                    ImGui.PopID();
                }
                ImGui.EndTable();
            }
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

    /**
     * Native buffers for the ECS window, allocated on first use by this holder class:
     * a {@code NativeBoolArray} created before {@code ImGuiGdx.load()} (e.g. in a field
     * of {@code DebugUI}, which {@code Main} constructs before {@code create()} crashes.
     */
    static final class EcsFlag {
        /** Window shown; the title-bar close button clears it, the "ecs" checkbox above sets it. */
        static final NativeBoolArray open = new NativeBoolArray(1);
        /** One checkbox buffer reused per table row; PushID keeps the repeated label unique. */
        static final NativeBoolArray systemEnabled = new NativeBoolArray(1);

        static {
            open.setValue(0, true);
        }
    }
}
