import SwiftUI

/// Swift/SwiftUI port of the Kotlin `Mooley` composable in
/// `composeApp/src/commonMain/.../Mooley.kt`. Same geometry ratios so both
/// platforms render an identical character; different rendering primitives
/// because iOS uses `SwiftUI.Path` instead of Compose's `DrawScope`.
struct MooleyView: View {
    let mood: MooleyMood
    var accent: MooleyAccent = .blue

    var body: some View {
        Canvas { ctx, size in
            let palette = MooleyPalette.forMood(mood: mood, accent: accent)
            let cx = size.width / 2
            let cy = size.height / 2
            let bodyRadius = min(size.width, size.height) * 0.42
            let faceScale = bodyRadius

            // Halo + body
            ctx.fill(
                Path(ellipseIn: CGRect(x: cx - bodyRadius * 1.15,
                                       y: cy - bodyRadius * 1.15,
                                       width: bodyRadius * 2.3,
                                       height: bodyRadius * 2.3)),
                with: .color(palette.bodyHighlight)
            )
            ctx.fill(
                Path(ellipseIn: CGRect(x: cx - bodyRadius, y: cy - bodyRadius,
                                       width: bodyRadius * 2, height: bodyRadius * 2)),
                with: .color(palette.body)
            )
            // Shine highlight
            let shineR = bodyRadius * 0.18
            ctx.fill(
                Path(ellipseIn: CGRect(x: cx - bodyRadius * 0.32 - shineR,
                                       y: cy - bodyRadius * 0.45 - shineR,
                                       width: shineR * 2, height: shineR * 2)),
                with: .color(palette.eye.opacity(0.25))
            )

            // Face
            let eyeGap = faceScale * 0.42
            let eyeCy = cy - faceScale * 0.10
            let eyeSize = faceScale * 0.14

            switch mood {
            case .happy:
                drawArcEyes(ctx: ctx, cx: cx, cy: eyeCy, gap: eyeGap, size: eyeSize, color: palette.mouth)
                drawSmile(ctx: ctx, cx: cx, cy: cy + faceScale * 0.25,
                          width: faceScale * 0.90, height: faceScale * 0.55,
                          stroke: faceScale * 0.11, color: palette.mouth, upward: true)
            case .neutral:
                drawDotEyes(ctx: ctx, cx: cx, cy: eyeCy, gap: eyeGap, size: eyeSize,
                            eyeWhite: palette.eye, pupil: palette.mouth)
                drawSmile(ctx: ctx, cx: cx, cy: cy + faceScale * 0.30,
                          width: faceScale * 0.60, height: faceScale * 0.28,
                          stroke: faceScale * 0.09, color: palette.mouth, upward: true)
            case .worried:
                drawDotEyes(ctx: ctx, cx: cx, cy: eyeCy, gap: eyeGap, size: eyeSize,
                            eyeWhite: palette.eye, pupil: palette.mouth)
                // Small O mouth as circle stroke
                ctx.stroke(
                    Path(ellipseIn: CGRect(x: cx - faceScale * 0.11, y: cy + faceScale * 0.30 - faceScale * 0.11,
                                            width: faceScale * 0.22, height: faceScale * 0.22)),
                    with: .color(palette.mouth), lineWidth: faceScale * 0.06
                )
                drawSweatDrop(ctx: ctx, cx: cx + faceScale * 0.65, cy: eyeCy - faceScale * 0.20,
                              height: faceScale * 0.22, color: palette.accessory)
            case .overBudget:
                drawXEyes(ctx: ctx, cx: cx, cy: eyeCy, gap: eyeGap, size: eyeSize, color: palette.mouth)
                drawSmile(ctx: ctx, cx: cx, cy: cy + faceScale * 0.35,
                          width: faceScale * 0.70, height: faceScale * 0.30,
                          stroke: faceScale * 0.11, color: palette.mouth, upward: false)
            }
        }
    }

    private func drawDotEyes(ctx: GraphicsContext, cx: CGFloat, cy: CGFloat, gap: CGFloat,
                             size: CGFloat, eyeWhite: Color, pupil: Color) {
        for x in [cx - gap, cx + gap] {
            ctx.fill(Path(ellipseIn: CGRect(x: x - size, y: cy - size,
                                            width: size * 2, height: size * 2)),
                     with: .color(eyeWhite))
            ctx.fill(Path(ellipseIn: CGRect(x: x - size * 0.55, y: cy - size * 0.55,
                                            width: size * 1.1, height: size * 1.1)),
                     with: .color(pupil))
        }
    }

    private func drawArcEyes(ctx: GraphicsContext, cx: CGFloat, cy: CGFloat, gap: CGFloat,
                             size: CGFloat, color: Color) {
        for x in [cx - gap, cx + gap] {
            var path = Path()
            let rect = CGRect(x: x - size, y: cy - size * 0.7,
                              width: size * 2, height: size * 1.4)
            path.addArc(
                center: CGPoint(x: rect.midX, y: rect.midY),
                radius: size,
                startAngle: .degrees(200),
                endAngle: .degrees(340),
                clockwise: false
            )
            ctx.stroke(path, with: .color(color), lineWidth: size * 0.55)
        }
    }

    private func drawXEyes(ctx: GraphicsContext, cx: CGFloat, cy: CGFloat, gap: CGFloat,
                           size: CGFloat, color: Color) {
        let len = size * 1.1
        for x in [cx - gap, cx + gap] {
            var p = Path()
            p.move(to: CGPoint(x: x - len, y: cy - len))
            p.addLine(to: CGPoint(x: x + len, y: cy + len))
            p.move(to: CGPoint(x: x - len, y: cy + len))
            p.addLine(to: CGPoint(x: x + len, y: cy - len))
            ctx.stroke(p, with: .color(color), lineWidth: size * 0.35)
        }
    }

    private func drawSmile(ctx: GraphicsContext, cx: CGFloat, cy: CGFloat,
                           width: CGFloat, height: CGFloat, stroke: CGFloat,
                           color: Color, upward: Bool) {
        var path = Path()
        let rect = CGRect(x: cx - width / 2, y: cy - height / 2,
                          width: width, height: height)
        path.addArc(
            center: CGPoint(x: rect.midX, y: rect.midY),
            radius: width / 2,
            startAngle: .degrees(upward ? 0 : 180),
            endAngle: .degrees(upward ? 180 : 360),
            clockwise: false
        )
        ctx.stroke(path, with: .color(color), lineWidth: stroke)
    }

    private func drawSweatDrop(ctx: GraphicsContext, cx: CGFloat, cy: CGFloat,
                               height: CGFloat, color: Color) {
        let bodyR = height * 0.32
        ctx.fill(Path(ellipseIn: CGRect(x: cx - bodyR, y: cy + height * 0.20 - bodyR,
                                        width: bodyR * 2, height: bodyR * 2)),
                 with: .color(color))
        var p = Path()
        p.move(to: CGPoint(x: cx - bodyR * 0.7, y: cy + height * 0.05))
        p.addLine(to: CGPoint(x: cx, y: cy - height * 0.45))
        p.addLine(to: CGPoint(x: cx + bodyR * 0.7, y: cy + height * 0.05))
        p.closeSubpath()
        ctx.fill(p, with: .color(color))
    }
}

// MARK: - Palette

enum MooleyAccent { case blue, green }

struct MooleyPalette {
    let body: Color
    let bodyHighlight: Color
    let eye: Color
    let mouth: Color
    let blush: Color
    let accessory: Color

    static func forMood(mood: MooleyMood, accent: MooleyAccent) -> MooleyPalette {
        let base: Color = accent == .blue
            ? Color(red: 0.208, green: 0.384, blue: 0.965)   // #3562F6
            : Color(red: 0.086, green: 0.639, blue: 0.290)   // #16A34A
        let body: Color
        switch mood {
        case .happy, .neutral: body = base
        case .worried: body = Color(red: 0.878, green: 0.659, blue: 0.043)  // #E0A80B
        case .overBudget: body = Color(red: 0.863, green: 0.149, blue: 0.149) // #DC2626
        }
        return MooleyPalette(
            body: body,
            bodyHighlight: body.opacity(0.35),
            eye: .white,
            mouth: Color(red: 0.122, green: 0.161, blue: 0.216), // #1F2937
            blush: mood == .happy
                ? Color(red: 0.976, green: 0.659, blue: 0.831).opacity(0.7)
                : Color.clear,
            accessory: mood == .worried
                ? Color(red: 0.376, green: 0.647, blue: 0.980) // #60A5FA
                : Color.clear
        )
    }
}
