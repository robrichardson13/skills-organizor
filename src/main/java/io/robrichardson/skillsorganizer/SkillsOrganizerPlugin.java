package io.robrichardson.skillsorganizer;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.widgets.*;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.callback.ClientThread;

import java.awt.*;
import java.util.Objects;

@Slf4j
@PluginDescriptor(
		name = "Skills Organizer"
)
public class SkillsOrganizerPlugin extends Plugin
{
	private static final int SCRIPTID_STATS_SKILLTOTAL = 396;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private SkillsOrganizerConfig config;

	private SkillWidgetGroup[] groups = new SkillWidgetGroup[SkillOrganizerData.values().length];

	@Override
	protected void startUp()
	{
		if (client.getGameState() == GameState.LOGGED_IN) {
			clientThread.invoke(this::updateSkillBars);
		}
	}

	@Override
	protected void shutDown()
	{
		clientThread.invoke(this::resetSkillBars);
	}

	@Provides
	SkillsOrganizerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(SkillsOrganizerConfig.class);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event) {
		if (!SkillsOrganizerConfig.GROUP.equals(event.getGroup())) return;

		clientThread.invoke(this::updateSkillBars);
	}

	@Subscribe
	public void onScriptPostFired(ScriptPostFired event) {
		if (event.getScriptId() == SCRIPTID_STATS_SKILLTOTAL) {
			updateSkillBars();
		}
	}

	private void updateSkillBars() {
		Widget skillsContainer = client.getWidget(WidgetInfo.SKILLS_CONTAINER);
		if (skillsContainer == null) {
			return;
		}

		for (Widget skillTile : skillsContainer.getStaticChildren()) {
			int idx = WidgetInfo.TO_CHILD(skillTile.getId()) - 1;
			SkillOrganizerData skillOrganizerData = SkillOrganizerData.get(idx);
			if(skillOrganizerData == null) continue;

			SkillsOrganizerConfig.SkillPositionOption skillPositionConfig = skillOrganizerData.getSkillPositionConfig().apply(config);
			SkillsOrganizerConfig.SkillVisibilityOption skillVisibilityOption = skillOrganizerData.getSkillVisibilityConfig().apply(config);

			skillTile.setXPositionMode(WidgetPositionMode.ABSOLUTE_LEFT);
			skillTile.setYPositionMode(WidgetPositionMode.ABSOLUTE_LEFT);
			skillTile.setOriginalX(skillPositionConfig.getX());
			skillTile.setOriginalY(skillPositionConfig.getY());
			skillTile.revalidate();

			SkillWidgetGroup widgetGroup = groups[idx];
			if(widgetGroup == null) {
				widgetGroup = buildWidgetGroup(skillTile);
			}
			Widget leftWidget = widgetGroup.getLeft();
			Widget rightWidget = widgetGroup.getRight();
			Widget darkenWidget = widgetGroup.getDarken();

			if(skillVisibilityOption == SkillsOrganizerConfig.SkillVisibilityOption.HIDE) {
				skillTile.setHidden(true);
				leftWidget.setHidden(true);
				rightWidget.setHidden(true);
				darkenWidget.setHidden(true);
			} else if(skillVisibilityOption == SkillsOrganizerConfig.SkillVisibilityOption.FADE) {
				skillTile.setHidden(false);
				leftWidget.setHidden(false);
				rightWidget.setHidden(false);
				darkenWidget.setHidden(true);
			} else if(skillVisibilityOption == SkillsOrganizerConfig.SkillVisibilityOption.DARK) {
				skillTile.setHidden(false);
				leftWidget.setHidden(true);
				rightWidget.setHidden(true);
				darkenWidget.setHidden(false);
			} else {
				skillTile.setHidden(false);
				leftWidget.setHidden(true);
				rightWidget.setHidden(true);
				darkenWidget.setHidden(true);
			}
		}
	}

	private SkillWidgetGroup buildWidgetGroup (Widget skillTile) {
		int idx = WidgetInfo.TO_CHILD(skillTile.getId()) - 1;

		Widget left = skillTile.createChild(-1, WidgetType.GRAPHIC);
		left.setSpriteId(174);
		left.setOriginalWidth(36);
		left.setOriginalHeight(36);
		left.setXPositionMode(WidgetPositionMode.ABSOLUTE_LEFT);
		left.setXPositionMode(WidgetPositionMode.ABSOLUTE_TOP);
		left.setOriginalX(-2);
		left.setOriginalY(-2);
		left.setOpacity(90);
		left.setHidden(true);
		left.revalidate();

		Widget right = skillTile.createChild(-1, WidgetType.GRAPHIC);
		right.setSpriteId(176);
		right.setOriginalWidth(36);
		right.setOriginalHeight(36);
		right.setXPositionMode(WidgetPositionMode.ABSOLUTE_LEFT);
		right.setXPositionMode(WidgetPositionMode.ABSOLUTE_TOP);
		right.setOriginalX(28);
		right.setOriginalY(-2);
		right.setOpacity(90);
		right.setHidden(true);
		right.revalidate();

		Widget darken = skillTile.createChild(-1, WidgetType.RECTANGLE);
		darken.setXPositionMode(WidgetPositionMode.ABSOLUTE_CENTER);
		darken.setYPositionMode(WidgetPositionMode.ABSOLUTE_CENTER);
		darken.setWidthMode(WidgetSizeMode.MINUS);
		darken.setHeightMode(WidgetSizeMode.MINUS);
		darken.setOriginalWidth(0);
		darken.setOriginalHeight(0);
		darken.setFilled(true);
		darken.setTextColor(Color.BLACK.getRGB());
		darken.setOpacity(90);
		darken.setHidden(true);
		darken.revalidate();


		SkillWidgetGroup newGroup = new SkillWidgetGroup(left, right, darken);
		groups[idx] = newGroup;

		return newGroup;
	}

	private void resetSkillBars() {
		Widget skillsContainer = client.getWidget(WidgetInfo.SKILLS_CONTAINER);
		if (skillsContainer == null) {
			return;
		}

		for (Widget skillTile : skillsContainer.getStaticChildren()) {
			int idx = WidgetInfo.TO_CHILD(skillTile.getId()) - 1;
			SkillOrganizerData skillOrganizerData = SkillOrganizerData.get(idx);
			if(skillOrganizerData == null) continue;

			// Reset position back to default
			skillTile.setOriginalX(skillOrganizerData.getDefaultPosition().getX());
			skillTile.setXPositionMode(WidgetPositionMode.ABSOLUTE_LEFT);

			skillTile.setOriginalY(skillOrganizerData.getDefaultPosition().getY());
			skillTile.setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP);

			skillTile.revalidate();

			// Show tile if hidden
			skillTile.setHidden(false);
		}

		for (SkillWidgetGroup widgetGroup : groups) {
			if (widgetGroup == null) {
				continue;
			}
			Widget parent = widgetGroup.getLeft().getParent();
			Widget[] children = parent.getChildren();
			for (int i = 0; i < Objects.requireNonNull(children).length; i++) {
				Widget child = children[i];
				if (widgetGroup.contains(child)) {
					children[i] = null;
				}
			}
		}

		groups = new SkillWidgetGroup[SkillOrganizerData.values().length];
	}
}
