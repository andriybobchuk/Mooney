package com.andriybobchuk.mooney.core.platform

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Decodes an encoded image byte array (JPEG / PNG / HEIC) into a Compose
 * [ImageBitmap]. Returns null on any decode failure — caller renders a
 * placeholder in that case.
 *
 * Platform-specific actuals: Android uses `BitmapFactory.decodeByteArray`
 * which handles JPEG + PNG + HEIC via the system codec; iOS uses Skia's
 * `Image.makeFromEncoded` which handles the same formats + Compose's
 * `toComposeImageBitmap` extension.
 */
expect fun decodeImageBitmap(bytes: ByteArray): ImageBitmap?
