package com.drunkard.client;

import com.drunkard.BarDialogue;
import com.drunkard.Medieval;
import com.drunkard.BarPatron;
import com.drunkard.DialogueMenu;
import com.drunkard.SkinPainter;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;

public class DialogueScreen extends AbstractContainerScreen<DialogueMenu> {
   private final List<Button> choices = new ArrayList<>();
   private int shownNode = -1;
   private long shownAt;
   private boolean answered;

   public DialogueScreen(DialogueMenu menu, Inventory inventory, Component title) {
      super(menu, inventory, title);
   }

   protected void m_7856_() {
      super.m_7856_();
      this.f_97726_ = this.f_96543_;
      this.f_97727_ = this.f_96544_;
      this.f_97735_ = 0;
      this.f_97736_ = 0;
      this.shownNode = -1;
      this.rebuild();
   }

   private void rebuild() {
      for (Button b : this.choices) {
         this.m_169411_(b);
      }

      this.choices.clear();
      this.shownNode = ((DialogueMenu)this.f_97732_).data.m_6413_(0);
      this.shownAt = System.currentTimeMillis();
      this.answered = false;
      BarDialogue.Node node = ((DialogueMenu)this.f_97732_).node();
      int w = Math.min(320, this.f_96543_ - 40);
      int x = (this.f_96543_ - w) / 2;
      if (node.end()) {
         this.choices
            .add(
               (Button)this.m_142416_(
                  Button.m_253074_(Component.m_237113_("Leave"), b -> this.press(91)).m_252987_(x + w / 2 - 50, this.f_96544_ - 34, 100, 20).m_253136_()
               )
            );
      } else {
         int n = node.options().size();

         for (int i = 0; i < n; i++) {
            int choice = i;
            this.choices
               .add(
                  (Button)this.m_142416_(
                     Button.m_253074_(Component.m_237113_(i + 1 + ". " + this.fill(node.options().get(i).text())), b -> this.choose(choice))
                        .m_252987_(x, this.f_96544_ - 14 - (n - i) * 22, w, 20)
                        .m_253136_()
                  )
               );
         }
      }
   }

   private void choose(int choice) {
      if (!this.answered) {
         this.answered = true;
         this.press(choice);
      }
   }

   private void press(int id) {
      if (this.f_96541_ != null && this.f_96541_.f_91072_ != null) {
         this.f_96541_.f_91072_.m_105208_(((DialogueMenu)this.f_97732_).f_38840_, id);
      }
   }

   private String fill(String text) {
      String drink = "drink";
      String other = null;
      int skin = 0;
      String bartender = this.f_96541_ != null && this.f_96541_.f_91074_ != null ? this.f_96541_.f_91074_.m_7755_().getString() : "friend";
      if (this.f_96541_ != null
         && this.f_96541_.f_91073_ != null
         && this.f_96541_.f_91073_.m_6815_(((DialogueMenu)this.f_97732_).entityId) instanceof BarPatron patron) {
         skin = patron.skin();
         if (patron.product() != null) {
            drink = patron.product().name().getString();
         }

         BarPatron nearest = null;
         double best = 196.0;

         for (BarPatron o : this.f_96541_.f_91073_.m_6443_(BarPatron.class, patron.m_20191_().m_82400_(14.0), ox -> ox != patron)) {
            double d = o.m_20280_(patron);
            if (d < best && d > 4.0) {
               best = d;
               nearest = o;
            }
         }

         if (nearest != null) {
            other = SkinPainter.describe(nearest.skin());
         }
      }

      return Medieval.speak(skin, BarDialogue.fill(text, drink, other, bartender));
   }

   protected void m_181908_() {
      super.m_181908_();
      if (((DialogueMenu)this.f_97732_).data.m_6413_(0) != this.shownNode) {
         this.rebuild();
      }

      if (!this.answered && !((DialogueMenu)this.f_97732_).node().end() && System.currentTimeMillis() - this.shownAt > 12000L) {
         this.answered = true;
         this.press(90);
      }
   }

   public boolean m_7933_(int key, int scan, int modifiers) {
      BarDialogue.Node node = ((DialogueMenu)this.f_97732_).node();
      if (!node.end() && key >= 49 && key < 49 + node.options().size()) {
         this.choose(key - 49);
         return true;
      } else if (!node.end() || key != 257 && key != 32) {
         return super.m_7933_(key, scan, modifiers);
      } else {
         this.press(91);
         return true;
      }
   }

   protected void m_7286_(GuiGraphics g, float partial, int mouseX, int mouseY) {
      BarDialogue.Node node = ((DialogueMenu)this.f_97732_).node();
      int n = node.end() ? 1 : node.options().size();
      int bandTop = this.f_96544_ - 14 - n * 22 - 52;
      g.m_280024_(0, bandTop - 30, this.f_96543_, bandTop, 0, -1610612736);
      g.m_280509_(0, bandTop, this.f_96543_, this.f_96544_, -1610612736);
      int w = Math.min(320, this.f_96543_ - 40);
      List<FormattedCharSequence> lines = this.f_96547_.m_92923_(Component.m_237113_("“" + this.fill(node.line()) + "”"), w);
      int y = bandTop + 8;

      for (FormattedCharSequence line : lines) {
         g.m_280649_(this.f_96547_, line, (this.f_96543_ - this.f_96547_.m_92724_(line)) / 2, y, 16777215, true);
         y += 11;
      }

      if (!node.end() && !this.answered) {
         float left = 1.0F - Math.min(1.0F, (float)(System.currentTimeMillis() - this.shownAt) / 12000.0F);
         int bar = (int)((float)w * left);
         int x = (this.f_96543_ - w) / 2;
         int by = this.f_96544_ - 14 - n * 22 - 8;
         g.m_280509_(x, by, x + w, by + 3, 1627389951);
         g.m_280509_(x, by, x + bar, by + 3, left > 0.3F ? -1513240 : -2074038);
      }
   }

   protected void m_280003_(GuiGraphics g, int mouseX, int mouseY) {
   }

   public boolean m_7043_() {
      return false;
   }
}
