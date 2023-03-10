package mtr.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import mtr.MTRClient;
import mtr.client.DoorAnimationType;
import mtr.client.IDrawing;
import mtr.client.ScrollingText;
import mtr.data.IGui;
import mtr.data.Route;
import mtr.data.Station;
import mtr.data.TrainClient;
import mtr.mappings.UtilitiesClient;
import mtr.render.RenderTrains;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public abstract class ModelSimpleTrainBase<T> extends ModelTrainBase {

	public ModelSimpleTrainBase(DoorAnimationType doorAnimationType, boolean renderDoorOverlay) {
		super(doorAnimationType, renderDoorOverlay);
	}

	@Override
	protected final void render(PoseStack matrices, VertexConsumer vertices, RenderStage renderStage, int light, float doorLeftX, float doorRightX, float doorLeftZ, float doorRightZ, int car, int totalCars, boolean head1IsFront, boolean renderDetails) {
		final boolean isEnd1Head = car == 0;
		final boolean isEnd2Head = car == totalCars - 1;

		for (final int position : getWindowPositions()) {
			renderWindowPositions(matrices, vertices, renderStage, light, position, renderDetails, doorLeftX, doorRightX, doorLeftZ, doorRightZ, isEnd1Head, isEnd2Head);
		}
		for (final int position : getDoorPositions()) {
			renderDoorPositions(matrices, vertices, renderStage, light, position, renderDetails, doorLeftX, doorRightX, doorLeftZ, doorRightZ, isEnd1Head, isEnd2Head);
		}

		if (isEnd1Head) {
			renderHeadPosition1(matrices, vertices, renderStage, light, getEndPositions()[0], renderDetails, doorLeftX, doorRightX, doorLeftZ, doorRightZ, head1IsFront);
		} else {
			renderEndPosition1(matrices, vertices, renderStage, light, getEndPositions()[0], renderDetails, doorLeftX, doorRightX, doorLeftZ, doorRightZ);
		}

		if (isEnd2Head) {
			renderHeadPosition2(matrices, vertices, renderStage, light, getEndPositions()[1], renderDetails, doorLeftX, doorRightX, doorLeftZ, doorRightZ, !head1IsFront);
		} else {
			renderEndPosition2(matrices, vertices, renderStage, light, getEndPositions()[1], renderDetails, doorLeftX, doorRightX, doorLeftZ, doorRightZ);
		}
	}

	@Override
	protected void renderExtraDetails1(PoseStack matrices, MultiBufferSource vertexConsumers, int light, int lightOnInteriorLevel, boolean lightsOn, float doorLeftX, float doorRightX, float doorLeftZ, float doorRightZ) {
		for (final int position : getDoorPositions()) {
			final ModelDoorOverlay modelDoorOverlay = renderDoorOverlay ? getModelDoorOverlay() : null;
			if (modelDoorOverlay != null) {
				modelDoorOverlay.render(matrices, vertexConsumers, RenderStage.INTERIOR, lightOnInteriorLevel, position, doorLeftX, doorRightX, doorLeftZ, doorRightZ, lightsOn);
				modelDoorOverlay.render(matrices, vertexConsumers, RenderStage.EXTERIOR, light, position, doorLeftX, doorRightX, doorLeftZ, doorRightZ, lightsOn);
			}
			final ModelDoorOverlayTopBase modelDoorOverlayTop = renderDoorOverlay ? getModelDoorOverlayTop() : null;
			if (modelDoorOverlayTop != null) {
				modelDoorOverlayTop.render(matrices, vertexConsumers, light, position, doorLeftX, doorRightX, doorLeftZ, doorRightZ);
			}
		}
	}

	@Override
	protected void renderExtraDetails2(PoseStack matrices, MultiBufferSource vertexConsumers, TrainClient train, int car, int totalCars, boolean atPlatform) {
		final MultiBufferSource.BufferSource immediate = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
		final Route thisRoute = train.getThisRoute();
		final Route nextRoute = train.getNextRoute();
		final Station thisStation = train.getThisStation();
		final Station nextStation = train.getNextStation();
		final Station lastStation = train.getLastStation();
		renderTextDisplays(matrices, vertexConsumers, Minecraft.getInstance().font, immediate, thisRoute, nextRoute, thisStation, nextStation, lastStation, thisRoute == null ? null : thisRoute.getDestination(train.getCurrentStationIndex()), car, totalCars, atPlatform, train.scrollingTexts);
		immediate.endBatch();
	}

	protected void renderTextDisplays(PoseStack matrices, MultiBufferSource vertexConsumers, Font font, MultiBufferSource.BufferSource immediate, Route thisRoute, Route nextRoute, Station thisStation, Station nextStation, Station lastStation, String customDestination, int car, int totalCars, boolean atPlatform, List<ScrollingText> scrollingTexts) {
	}

	protected void renderFrontDestination(PoseStack matrices, Font font, MultiBufferSource.BufferSource immediate, float x1, float y1, float z1, float x2, float y2, float z2, float rotationX, float rotationY, float maxWidth, float maxHeight, int colorCjk, int color, float fontSizeRatio, String text, boolean padOneLine, int car, int totalCars) {
		final boolean isEnd1Head = car == 0;
		final boolean isEnd2Head = car == totalCars - 1;

		for (int i = 0; i < 2; i++) {
			if (i == 0 && isEnd1Head || i == 1 && isEnd2Head) {
				matrices.pushPose();
				if (i == 1) {
					UtilitiesClient.rotateYDegrees(matrices, 180);
				}
				matrices.translate(x1, y1, z1);
				if (rotationY != 0) {
					UtilitiesClient.rotateYDegrees(matrices, rotationY);
				}
				if (rotationX != 0) {
					UtilitiesClient.rotateXDegrees(matrices, rotationX);
				}
				matrices.translate(x2, y2, z2);
				IDrawing.drawStringWithFont(matrices, font, immediate, text, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, HorizontalAlignment.CENTER, 0, 0, maxWidth, (padOneLine && !text.contains("|") ? IGui.isCjk(text) ? fontSizeRatio / (fontSizeRatio + 1) : 0.5F : 1) * maxHeight, 1, colorCjk, color, fontSizeRatio, false, MAX_LIGHT_GLOWING, null);
				matrices.popPose();
			}
		}
	}

	protected String getDestinationString(Station station, String customDestination, TextSpacingType textSpacingType, boolean toUpperCase) {
		final String text = customDestination == null ? station == null ? defaultDestinationString() : station.name : customDestination;
		final String finalResult;

		if (textSpacingType == TextSpacingType.NORMAL) {
			finalResult = text;
		} else {
			final String[] textSplit = text.split("\\|");
			final List<String> result = new ArrayList<>();
			boolean hasCjk = false;

			for (final String textPart : textSplit) {
				final boolean isCjk = IGui.isCjk(textPart);
				if (textSpacingType == TextSpacingType.SPACE_CJK || textSpacingType == TextSpacingType.SPACE_CJK_FLIPPED) {
					result.add(textSpacingType == TextSpacingType.SPACE_CJK ? result.size() : 0, isCjk && textPart.length() == 2 ? textPart.charAt(0) + " " + textPart.charAt(1) : textPart);
				} else if (textSpacingType == TextSpacingType.SPACE_CJK_LARGE) {
					if (isCjk) {
						final StringBuilder cjkResult = new StringBuilder();
						for (int i = 0; i < textPart.length(); i++) {
							cjkResult.append(textPart.charAt(i));
							for (int j = 0; j < (textPart.length() == 2 ? 3 : 1); j++) {
								cjkResult.append("   ");
							}
						}
						result.add(cjkResult.toString().trim());
					} else {
						result.add(textPart);
					}
				} else if (textSpacingType == TextSpacingType.MLR_SPACING) {
					final StringBuilder stringBuilder;
					if (isCjk) {
						stringBuilder = new StringBuilder(textPart);
						for (int i = textPart.length(); i < 3; i++) {
							stringBuilder.append(" ");
						}
						hasCjk = true;
					} else {
						stringBuilder = new StringBuilder();
						for (int i = textPart.length(); i < 9; i++) {
							stringBuilder.append(" ");
						}
						stringBuilder.append(textPart);
					}
					result.add(stringBuilder.toString());
				}
			}

			if (!hasCjk && textSpacingType == TextSpacingType.MLR_SPACING) {
				result.add(0, " ");
				result.add(0, " ");
			}

			finalResult = String.join("|", result);
		}

		return toUpperCase ? finalResult.toUpperCase(Locale.ENGLISH) : finalResult;
	}

	protected String defaultDestinationString() {
		return "";
	}

	public abstract T createNew(DoorAnimationType doorAnimationType, boolean renderDoorOverlay);

	protected abstract void renderWindowPositions(PoseStack matrices, VertexConsumer vertices, RenderStage renderStage, int light, int position, boolean renderDetails, float doorLeftX, float doorRightX, float doorLeftZ, float doorRightZ, boolean isEnd1Head, boolean isEnd2Head);

	protected abstract void renderDoorPositions(PoseStack matrices, VertexConsumer vertices, RenderStage renderStage, int light, int position, boolean renderDetails, float doorLeftX, float doorRightX, float doorLeftZ, float doorRightZ, boolean isEnd1Head, boolean isEnd2Head);

	protected abstract void renderHeadPosition1(PoseStack matrices, VertexConsumer vertices, RenderStage renderStage, int light, int position, boolean renderDetails, float doorLeftX, float doorRightX, float doorLeftZ, float doorRightZ, boolean useHeadlights);

	protected abstract void renderHeadPosition2(PoseStack matrices, VertexConsumer vertices, RenderStage renderStage, int light, int position, boolean renderDetails, float doorLeftX, float doorRightX, float doorLeftZ, float doorRightZ, boolean useHeadlights);

	protected abstract void renderEndPosition1(PoseStack matrices, VertexConsumer vertices, RenderStage renderStage, int light, int position, boolean renderDetails, float doorLeftX, float doorRightX, float doorLeftZ, float doorRightZ);

	protected abstract void renderEndPosition2(PoseStack matrices, VertexConsumer vertices, RenderStage renderStage, int light, int position, boolean renderDetails, float doorLeftX, float doorRightX, float doorLeftZ, float doorRightZ);

	protected abstract ModelDoorOverlay getModelDoorOverlay();

	protected abstract ModelDoorOverlayTopBase getModelDoorOverlayTop();

	protected abstract int[] getWindowPositions();

	protected abstract int[] getDoorPositions();

	protected abstract int[] getEndPositions();

	protected static String getAlternatingString(String text) {
		final String[] textSplit = text.split("\\|");
		return textSplit[((int) Math.floor(MTRClient.getGameTick() / 30)) % textSplit.length];
	}

	protected static String getLondonNextStationString(Route thisRoute, Route nextRoute, Station thisStation, Station nextStation, Station lastStation, String destinationString, boolean atPlatform) {
		final Station station = atPlatform ? thisStation : nextStation;
		if (station == null || thisRoute == null) {
			return "";
		} else {
			final List<String> messages = new ArrayList<>();
			final boolean isTerminating = lastStation != null && station.id == lastStation.id && nextRoute == null;

			if (!isTerminating) {
				messages.add(IGui.insertTranslation("gui.mtr.london_train_route_announcement_cjk", "gui.mtr.london_train_route_announcement", 2, IGui.textOrUntitled(thisRoute.name), IGui.textOrUntitled(destinationString)));
			}

			if (atPlatform) {
				messages.add(IGui.insertTranslation("gui.mtr.london_train_this_station_announcement_cjk", "gui.mtr.london_train_this_station_announcement", 1, IGui.textOrUntitled(station.name)));
			} else {
				messages.add(IGui.insertTranslation("gui.mtr.london_train_next_station_announcement_cjk", "gui.mtr.london_train_next_station_announcement", 1, IGui.textOrUntitled(station.name)));
			}

			final String mergedInterchangeRoutes = RenderTrains.getInterchangeRouteNames(station, thisRoute, nextRoute);
			if (!mergedInterchangeRoutes.isEmpty()) {
				messages.add(IGui.insertTranslation("gui.mtr.london_train_interchange_announcement_cjk", "gui.mtr.london_train_interchange_announcement", 1, mergedInterchangeRoutes));
			}

			if (isTerminating) {
				messages.add(IGui.insertTranslation("gui.mtr.london_train_terminating_announcement_cjk", "gui.mtr.london_train_terminating_announcement", 1, IGui.textOrUntitled(station.name)));
			}

			return IGui.formatStationName(IGui.mergeStations(messages, "", " "));
		}
	}

	protected enum TextSpacingType {NORMAL, SPACE_CJK, SPACE_CJK_FLIPPED, SPACE_CJK_LARGE, MLR_SPACING}
}
