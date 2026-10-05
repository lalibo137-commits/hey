package com.drunkard;

import com.mojang.logging.LogUtils;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import org.slf4j.Logger;

/**
 * Compact diagnostics for the pub (barkeeps, customers, registers), written to the game log with a "[Drunkard pub]"
 * prefix. Repeats of the same message from the same source are suppressed for a while so the log stays readable.
 */
public final class PubDebug {
   private static final Logger LOG = LogUtils.getLogger();
   private static final Map<String, Long> LAST = new ConcurrentHashMap<>();
   private static final long QUIET_MS = 10000L;

   public static void log(Entity who, String what) {
      String name = who instanceof BarWorker ? "barkeep" : (who instanceof BarPatron ? "customer" : who.getClass().getSimpleName());
      emit(name + "#" + who.getId(), name + "#" + who.getId() + " at " + who.blockPosition().toShortString() + ": " + what, what);
   }

   public static void log(BlockPos where, String source, String what) {
      emit(source + "@" + where.asLong(), source + " at " + where.toShortString() + ": " + what, what);
   }

   private static void emit(String key, String line, String what) {
      long now = System.currentTimeMillis();
      Long last = LAST.put(key + "|" + what, now);
      if (last == null || now - last >= QUIET_MS) {
         LOG.info("[Drunkard pub] " + line);
      }

      if (LAST.size() > 5000) {
         LAST.clear();
      }
   }

   public static void error(String where, Throwable t) {
      LOG.warn("[Drunkard pub] error in " + where, t);
   }

   private PubDebug() {
   }
}
