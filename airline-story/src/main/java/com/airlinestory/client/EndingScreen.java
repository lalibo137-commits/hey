package com.airlinestory.client;

import com.airlinestory.net.Net;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/** The epilogue, page by page, then the credits. */
public class EndingScreen extends Screen {
    private final List<String[]> pages = new ArrayList<>();
    private int page;
    private int age;
    private boolean done;

    public EndingScreen(String kind, boolean pip, float rating) {
        super(Component.literal("Epilogue"));
        if (kind.equals("stay")) {
            pages.add(new String[]{"ENDING: TIDEFOLK", "The boat drifts out past the reef on the morning tide, empty, its striped sail bright against the grey."});
            pages.add(new String[]{"", "Up on the mountain, the screens show a beach where nothing happens. Days of it. Weeks."});
            pages.add(new String[]{"", "The viewers drift away, the way viewers do. Ratings fall. One night, the hum under the island stops, and nobody notices for a week."});
            pages.add(new String[]{"", pip ? "Pip teaches you to fish properly. You teach Pip to count, and then, after some thought, you teach Pip not to."
                    : "Nalu teaches you to fish properly. Mira teaches you which leaves keep the fever away. Ahu teaches you nothing, very patiently."});
            pages.add(new String[]{"", "You lose track of the days. It's better."});
        } else if (kind.equals("free")) {
            pages.add(new String[]{"ENDING: UNRATED", "Nine days after you leave the reef behind, a fishing trawler finds a patched little boat with a striped sail."});
            pages.add(new String[]{"", pip
                    ? "The crew wrap you both in blankets. Pip asks the captain how many stars he is. He doesn't understand the question. Pip laughs for an hour."
                    : "The crew wrap you in a blanket and ask your name. You tell them. It takes them a long time to find you in any system."});
            pages.add(new String[]{"", "Meridian Air denies that Flight 417 ever existed. There is no Port Ellery. There is no Experience Lab 4. There never was."});
            pages.add(new String[]{"", "But seven million people saw a broadcast cut to black in the middle of a sentence. Some of them start asking questions."});
            pages.add(new String[]{"", "You never get your rating back. Nobody can rate a person who isn't in the system."});
            pages.add(new String[]{"", "For the first time in years, when someone smiles at you, you can't tell what it's worth. You decide that you like that."});
        } else if (rating >= 4.5f) {
            pages.add(new String[]{"ENDING: FIVE STARS", "You sail past the reef with a rating of " + String.format(java.util.Locale.ROOT, "%.2f", rating) + ". The audience goes wild."});
            pages.add(new String[]{"", "A Meridian launch is waiting on the other side. They give you a towel, a new uniform, and a name badge with your face on it."});
            pages.add(new String[]{"", "Graduates make the best cabin crew. They know exactly how the passengers feel."});
            pages.add(new String[]{"", "Your first flight as a Meridian Sky Host is Flight 417 to Port Ellery. Seat 14A has asked for the window."});
            pages.add(new String[]{"", "You smile at them. You mean it. You think you mean it."});
        } else {
            pages.add(new String[]{"ENDING: LAST CALL", "Somewhere past the reef the fog comes in thick and white, and the sea goes quiet."});
            pages.add(new String[]{"", "When it lifts, there is carpet under your feet instead of planks. Departure boards. The smell of coffee."});
            pages.add(new String[]{"", "Your SmileScore was uploaded. It wasn't high enough to graduate. Rehabilitation Experiences can be repeated as often as necessary."});
            pages.add(new String[]{"", pip ? "Somewhere behind you, a child asks their mother why everyone is smiling. Nobody answers."
                    : "Somewhere behind you, an old woman with knitting watches you walk in. She looks very tired."});
            pages.add(new String[]{"", "Iteration 5 begins at 6:42 AM."});
        }
        pages.add(new String[]{"AIRLINE STORY: FLIGHT 417", "Thank you for flying Meridian.\n\nWritten, built and haunted for Minecraft Forge 1.20.1.\n\nEvery interaction was rated."});
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.literal("Continue"), b -> next()).bounds(width / 2 - 50, height - 40, 100, 20).build());
    }

    private void next() {
        age = 0;
        page++;
        if (page >= pages.size()) {
            if (!done) Net.toServer(new Net.EndingDone());
            done = true;
            ClientFx.black = false;
            minecraft.setScreen(null);
        }
    }

    @Override
    public void tick() {
        age++;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics g) {
        g.fill(0, 0, width, height, 0xFF000000);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderBackground(g);
        if (page >= pages.size()) return;
        String[] p = pages.get(page);
        float a = Math.min(1f, (age + partial) / 30f);
        int alpha = VanillaGui.alpha(a);
        int y = height / 3;
        if (!p[0].isEmpty()) {
            g.pose().pushPose();
            g.pose().translate(width / 2f, y - 30, 0);
            g.pose().scale(2f, 2f, 1f);
            g.drawCenteredString(font, p[0], 0, 0, 0xFFFFFF | alpha);
            g.pose().popPose();
        }
        for (FormattedCharSequence line : VanillaGui.wrap(font, p[1], Math.min(320, width - 40))) {
            g.drawCenteredString(font, line, width / 2, y, 0xCCCCCC | alpha);
            y += 12;
        }
        super.render(g, mouseX, mouseY, partial);
    }
}
