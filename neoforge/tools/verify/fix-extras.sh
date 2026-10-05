#!/bin/bash
# 1.21.1 corrections to ForgeExtras output (names renamed since 1.20.1 / bad alignment, verified with Verify.java)
sed -i -e '/\tnoSummon\t(I)/d' -e 's/\tsaturationMod\t/\tsaturationModifier\t/' \
  -e 's#^\(net/minecraft/network/FriendlyByteBuf\twrite[A-Za-z]*\t([^)]*)\)Lio/netty/buffer/ByteBuf;#\1Lnet/minecraft/network/FriendlyByteBuf;#' "$1"
