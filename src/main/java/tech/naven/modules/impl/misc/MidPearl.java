package tech.naven.modules.impl.misc;

import tech.mixin.accessors.MultiPlayerGameModeAccessor;
import tech.naven.events.api.EventTarget;
import tech.naven.events.api.types.EventType;
import tech.naven.events.impl.EventRunTicks;
import tech.naven.modules.Category;
import tech.naven.modules.Module;
import tech.naven.modules.ModuleInfo;
import tech.naven.utils.MathUtils;
import tech.naven.utils.TimeHelper;
import tech.naven.values.ValueBuilder;
import tech.naven.values.impl.FloatValue;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

@ModuleInfo(
   name = "MidPearl",
   description = "Middle-click to throw an ender pearl",
   category = Category.MISC
)
public class MidPearl extends Module {
   private boolean doWork;
   private boolean switchBack;
   private int lastSlot = -1;

   @Override
   public void onEnable() {
      this.lastSlot = -1;
      this.doWork = false;
      this.switchBack = false;
   }

   @Override
   public void onDisable() {
      if (this.switchBack && mc.player != null && this.lastSlot >= 0) {
         mc.player.getInventory().setSelectedSlot(this.lastSlot);
      }

      this.switchBack = false;
      this.doWork = false;
      this.lastSlot = -1;
   }

   @EventTarget
   public void onTick(EventRunTicks event) {
      if (event.getType() != EventType.PRE) {
         return;
      }

      if (mc.player == null || mc.level == null || mc.gameMode == null || !mc.player.isAlive()) {
         return;
      }

      if (mc.options.keyPickItem.isDown()) {
         this.doWork = true;
         this.switchBack = false;
      }

      if (this.doWork) {
         int pearlSlot = this.findPearlSlot();
         if (pearlSlot != -1) {
            this.lastSlot = mc.player.getInventory().getSelectedSlot();
            mc.player.getInventory().setSelectedSlot(pearlSlot);
            ((MultiPlayerGameModeAccessor)mc.gameMode).invokeEnsureHasSentCarriedItem();
            mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
            this.doWork = false;
            this.switchBack = true;
         }
      }

      if (this.switchBack) {
         if (this.lastSlot >= 0) {
            mc.player.getInventory().setSelectedSlot(this.lastSlot);
         }

         this.switchBack = false;
      }
   }

   private int findPearlSlot() {
      if (mc.player == null) {
         return -1;
      }

      for (int i = 0; i <= 8; i++) {
         ItemStack stack = mc.player.getInventory().getItem(i);
         if (!stack.isEmpty() && stack.getItem() == Items.ENDER_PEARL) {
            return i;
         }
      }
      return -1;
   }
}
