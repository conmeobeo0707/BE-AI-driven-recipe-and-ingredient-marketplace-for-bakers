package com.manhnv.capstonebakerai.recipe.video;

import java.util.Map;

public final class VideoArt {

    private static final String LINE = "#EAD3C8";

    private static final String SPARKS =
            "<path class='spark' d='M70 150 L78 172 L100 180 L78 188 L70 210 L62 188 L40 180 L62 172 Z' fill='{{GOLD}}'/>"
            + "<path class='spark' d='M520 90 L526 106 L542 112 L526 118 L520 134 L514 118 L498 112 L514 106 Z' fill='{{GOLD}}'/>"
            + "<circle class='spark' cx='540' cy='300' r='9' fill='{{ACCENT}}' opacity='.5'/>";

    private static final String PLATE =
            "<ellipse id='plate' cx='300' cy='520' rx='260' ry='38' fill='#FFFFFF' stroke='" + LINE + "' stroke-width='4'/>";

    private static String berry(int x, int y) {
        return "<g class='pop'><path d='M" + x + " " + y + " C" + (x - 25) + " " + y + " " + (x - 23) + " " + (y + 40)
                + " " + x + " " + (y + 48) + " C" + (x + 23) + " " + (y + 40) + " " + (x + 25) + " " + y + " " + x + " " + y
                + " Z' fill='{{ACCENT}}'/><path d='M" + (x - 12) + " " + (y + 2) + " L" + x + " " + (y - 10) + " L"
                + (x + 12) + " " + (y + 2) + " Z' fill='#6BA368'/></g>";
    }

    private static final Map<String, String> HEROES = Map.of(
            "cake", SPARKS + PLATE
                    + "<g class='pop'><rect x='100' y='370' width='400' height='145' rx='26' fill='#FFFFFF' stroke='" + LINE
                    + "' stroke-width='4'/><rect x='104' y='430' width='392' height='26' fill='{{BLUSH}}'/></g>"
                    + "<g class='pop'><rect x='140' y='250' width='320' height='130' rx='26' fill='#FFFFFF' stroke='" + LINE
                    + "' stroke-width='4'/><rect x='144' y='320' width='312' height='22' fill='{{BLUSH}}'/></g>"
                    + "<path class='pop' d='M140 276 Q140 250 166 250 L434 250 Q460 250 460 276 L460 290 Q450 312 440 290 "
                    + "Q430 272 418 300 Q406 326 394 296 Q382 272 368 292 Q354 312 340 290 Q326 272 312 304 Q298 330 284 298 "
                    + "Q272 274 258 292 Q244 312 230 290 Q216 270 204 300 Q192 326 180 294 Q168 270 156 292 Q148 306 140 290 Z' "
                    + "fill='{{ACCENT}}' opacity='.45'/>"
                    + berry(180, 222) + berry(240, 222) + berry(360, 222) + berry(420, 222)
                    + "<g class='pop'><rect x='292' y='150' width='16' height='100' rx='6' fill='{{GOLD}}'/>"
                    + "<ellipse id='flame' cx='300' cy='130' rx='11' ry='20' fill='#FFB547'/>"
                    + "<ellipse cx='300' cy='137' rx='5' ry='9' fill='#FFF1C9'/></g>",
            "cupcake", SPARKS + PLATE
                    + "<path class='pop' d='M175 330 L425 330 L395 505 L205 505 Z' fill='{{BLUSH}}' stroke='" + LINE + "' stroke-width='4'/>"
                    + "<path class='pop' d='M215 335 L232 500 M265 335 L272 500 M300 335 L300 500 M335 335 L328 500 M385 335 L368 500' "
                    + "stroke='#FFFFFF' stroke-width='8' stroke-linecap='round'/>"
                    + "<ellipse class='pop' cx='300' cy='322' rx='150' ry='52' fill='#FFFFFF' stroke='" + LINE + "' stroke-width='4'/>"
                    + "<ellipse class='pop' cx='300' cy='268' rx='115' ry='46' fill='#FFFFFF' stroke='" + LINE + "' stroke-width='4'/>"
                    + "<ellipse class='pop' cx='300' cy='220' rx='72' ry='38' fill='#FFFFFF' stroke='" + LINE + "' stroke-width='4'/>"
                    + "<g class='pop'><circle cx='300' cy='168' r='26' fill='{{ACCENT}}'/>"
                    + "<path d='M300 142 Q310 112 335 104' stroke='#6BA368' stroke-width='6' fill='none' stroke-linecap='round'/></g>"
                    + "<g class='pop'><rect x='240' y='250' width='16' height='6' rx='3' fill='{{GOLD}}' transform='rotate(25 248 253)'/>"
                    + "<rect x='330' y='300' width='16' height='6' rx='3' fill='{{ACCENT}}' transform='rotate(-30 338 303)'/>"
                    + "<rect x='280' y='205' width='16' height='6' rx='3' fill='#6BA368' transform='rotate(60 288 208)'/></g>",
            "bread", SPARKS
                    + "<rect id='plate' x='70' y='470' width='460' height='60' rx='22' fill='#E9C9A3' stroke='#C79A6B' stroke-width='4'/>"
                    + "<path class='pop' d='M110 470 Q100 300 300 280 Q500 300 490 470 Z' fill='{{GOLD}}' stroke='#B57A35' stroke-width='5'/>"
                    + "<path class='pop' d='M190 330 Q215 360 205 400 M270 310 Q295 345 285 395 M350 315 Q375 350 365 400 M425 340 Q445 365 438 405' "
                    + "stroke='#FFF1D6' stroke-width='12' fill='none' stroke-linecap='round'/>"
                    + "<g class='pop'><path d='M470 250 L520 120' stroke='{{ACCENT}}' stroke-width='6' stroke-linecap='round'/>"
                    + "<ellipse cx='505' cy='160' rx='9' ry='18' fill='{{ACCENT}}' transform='rotate(22 505 160)'/>"
                    + "<ellipse cx='495' cy='190' rx='9' ry='18' fill='{{ACCENT}}' transform='rotate(22 495 190)'/>"
                    + "<ellipse cx='516' cy='132' rx='8' ry='16' fill='{{ACCENT}}' transform='rotate(22 516 132)'/></g>",
            "donut", SPARKS + PLATE
                    + "<path class='pop' fill-rule='evenodd' d='M300 230 A170 150 0 1 1 299.9 230 Z M300 340 A60 45 0 1 0 300.1 340 Z' "
                    + "fill='{{GOLD}}' stroke='#B57A35' stroke-width='5'/>"
                    + "<path class='pop' fill-rule='evenodd' d='M300 245 C410 245 465 300 455 370 C448 410 430 400 420 420 C405 450 380 425 360 450 "
                    + "C340 470 320 440 300 455 C270 470 260 440 235 450 C205 460 200 425 180 420 C150 410 140 380 145 360 C150 300 200 245 300 245 Z "
                    + "M300 340 A60 45 0 1 0 300.1 340 Z' fill='{{ACCENT}}'/>"
                    + "<g class='pop'><rect x='220' y='290' width='22' height='8' rx='4' fill='#FFFFFF' transform='rotate(30 231 294)'/>"
                    + "<rect x='370' y='300' width='22' height='8' rx='4' fill='{{GOLD}}' transform='rotate(-40 381 304)'/>"
                    + "<rect x='300' y='270' width='22' height='8' rx='4' fill='#6BA368' transform='rotate(10 311 274)'/>"
                    + "<rect x='400' y='380' width='22' height='8' rx='4' fill='#FFFFFF' transform='rotate(70 411 384)'/>"
                    + "<rect x='190' y='380' width='22' height='8' rx='4' fill='{{GOLD}}' transform='rotate(-20 201 384)'/></g>",
            "cookie", SPARKS + PLATE
                    + "<g class='pop'><circle cx='230' cy='400' r='115' fill='{{GOLD}}' stroke='#B57A35' stroke-width='5'/>"
                    + "<circle cx='190' cy='370' r='14' fill='{{INK}}'/><circle cx='260' cy='350' r='11' fill='{{INK}}'/>"
                    + "<circle cx='240' cy='430' r='13' fill='{{INK}}'/><circle cx='170' cy='440' r='9' fill='{{INK}}'/>"
                    + "<circle cx='290' cy='410' r='10' fill='{{INK}}'/></g>"
                    + "<g class='pop'><circle cx='390' cy='330' r='90' fill='{{GOLD}}' stroke='#B57A35' stroke-width='5' opacity='.92'/>"
                    + "<circle cx='370' cy='300' r='11' fill='{{INK}}'/><circle cx='420' cy='340' r='12' fill='{{INK}}'/>"
                    + "<circle cx='380' cy='370' r='9' fill='{{INK}}'/></g>"
                    + "<g class='pop'><rect x='440' y='170' width='90' height='140' rx='14' fill='#FFFFFF' stroke='" + LINE + "' stroke-width='5'/>"
                    + "<rect x='446' y='215' width='78' height='89' rx='10' fill='{{BLUSH}}'/></g>",
            "macaron", SPARKS + PLATE
                    + macaron(300, 455, "{{ACCENT}}") + macaron(300, 365, "{{GOLD}}") + macaron(300, 275, "#B9DDB0")
                    + macaron(300, 185, "{{BLUSH}}"));

    private static String macaron(int cx, int cy, String color) {
        return "<g class='pop'><ellipse cx='" + cx + "' cy='" + (cy - 22) + "' rx='130' ry='34' fill='" + color + "' stroke='"
                + LINE + "' stroke-width='4'/><rect x='" + (cx - 118) + "' y='" + (cy - 12) + "' width='236' height='22' rx='11' "
                + "fill='#FFFFFF'/><ellipse cx='" + cx + "' cy='" + (cy + 22) + "' rx='130' ry='34' fill='" + color
                + "' stroke='" + LINE + "' stroke-width='4'/></g>";
    }

    private static final Map<String, String> ICONS = Map.ofEntries(
            Map.entry("oven", "<g class='pop'><rect x='70' y='80' width='260' height='250' rx='26' fill='#FFFFFF' stroke='" + LINE + "' stroke-width='6'/>"
                    + "<rect x='70' y='80' width='260' height='60' rx='26' fill='{{BLUSH}}'/>"
                    + "<circle cx='120' cy='110' r='12' fill='{{ACCENT}}'/><circle cx='165' cy='110' r='12' fill='{{GOLD}}'/>"
                    + "<rect x='240' y='102' width='60' height='16' rx='8' fill='{{INK}}' opacity='.3'/></g>"
                    + "<g class='pop'><rect x='100' y='165' width='200' height='130' rx='16' fill='{{INK}}' opacity='.85'/>"
                    + "<rect x='130' y='240' width='140' height='34' rx='12' fill='{{GOLD}}'/>"
                    + "<path d='M150 205 Q160 190 150 175 M200 205 Q210 190 200 175 M250 205 Q260 190 250 175' stroke='#FFB547' "
                    + "stroke-width='7' fill='none' stroke-linecap='round'/></g>"),
            Map.entry("bowl", "<g class='pop'><path d='M60 200 L340 200 Q330 330 200 340 Q70 330 60 200 Z' fill='#FFFFFF' stroke='" + LINE + "' stroke-width='6'/>"
                    + "<path d='M75 225 L325 225' stroke='{{BLUSH}}' stroke-width='18' stroke-linecap='round'/></g>"
                    + "<g class='pop'><ellipse cx='200' cy='200' rx='140' ry='26' fill='{{BLUSH}}' stroke='" + LINE + "' stroke-width='6'/>"
                    + "<ellipse cx='175' cy='196' rx='30' ry='9' fill='#FFF1C9'/><ellipse cx='230' cy='200' rx='26' ry='8' fill='#FFF1C9'/></g>"
                    + "<g class='pop'><path d='M240 60 L205 190' stroke='{{GOLD}}' stroke-width='12' stroke-linecap='round'/>"
                    + "<path d='M210 175 Q170 140 200 110 Q230 140 210 175 M210 175 Q240 135 270 160 Q250 190 210 175' stroke='{{INK}}' "
                    + "stroke-width='6' fill='none' opacity='.6'/></g>"),
            Map.entry("sieve", "<g class='pop'><circle cx='180' cy='170' r='110' fill='#FFFFFF' stroke='{{INK}}' stroke-width='8' opacity='.9'/>"
                    + "<path d='M100 120 L260 120 M85 160 L275 160 M85 200 L275 200 M100 240 L260 240 M130 70 L130 270 M180 60 L180 280 M230 70 L230 270' "
                    + "stroke='" + LINE + "' stroke-width='4'/></g>"
                    + "<g class='pop'><path d='M285 170 L360 150' stroke='{{GOLD}}' stroke-width='16' stroke-linecap='round'/></g>"
                    + "<g class='pop'><circle cx='150' cy='315' r='9' fill='{{BLUSH}}'/><circle cx='190' cy='335' r='11' fill='{{BLUSH}}'/>"
                    + "<circle cx='220' cy='310' r='7' fill='{{BLUSH}}'/><ellipse cx='180' cy='365' rx='110' ry='18' fill='{{BLUSH}}'/></g>"),
            Map.entry("piping", "<g class='pop'><path d='M90 70 L280 70 L200 290 L170 290 Z' fill='#FFFFFF' stroke='" + LINE + "' stroke-width='6'/>"
                    + "<path d='M110 90 L260 90 L200 250 L170 250 Z' fill='{{BLUSH}}'/></g>"
                    + "<g class='pop'><path d='M168 290 L202 290 L192 330 L178 330 Z' fill='{{GOLD}}'/></g>"
                    + "<g class='pop'><path d='M130 370 Q150 330 185 345 Q220 330 240 370 Z' fill='#FFFFFF' stroke='" + LINE + "' stroke-width='5'/>"
                    + "<circle cx='300' cy='120' r='18' fill='{{ACCENT}}'/><circle cx='70' cy='200' r='12' fill='{{GOLD}}'/></g>"),
            Map.entry("rolling", "<g class='pop'><ellipse cx='200' cy='290' rx='160' ry='60' fill='{{GOLD}}' opacity='.55'/>"
                    + "<ellipse cx='200' cy='285' rx='130' ry='44' fill='#FFF1D6'/></g>"
                    + "<g class='pop'><rect x='70' y='175' width='260' height='60' rx='30' fill='#FFFFFF' stroke='" + LINE + "' stroke-width='6'/>"
                    + "<rect x='20' y='192' width='60' height='26' rx='13' fill='{{ACCENT}}'/>"
                    + "<rect x='320' y='192' width='60' height='26' rx='13' fill='{{ACCENT}}'/></g>"
                    + "<g class='pop'><circle cx='120' cy='110' r='8' fill='{{BLUSH}}'/><circle cx='280' cy='120' r='10' fill='{{BLUSH}}'/></g>"),
            Map.entry("fridge", "<g class='pop'><rect x='110' y='50' width='180' height='310' rx='24' fill='#FFFFFF' stroke='" + LINE + "' stroke-width='6'/>"
                    + "<path d='M110 160 L290 160' stroke='" + LINE + "' stroke-width='6'/>"
                    + "<rect x='130' y='85' width='14' height='50' rx='7' fill='{{INK}}' opacity='.35'/>"
                    + "<rect x='130' y='185' width='14' height='70' rx='7' fill='{{INK}}' opacity='.35'/></g>"
                    + "<g class='pop'><path d='M330 90 L330 170 M295 110 L365 150 M295 150 L365 110' stroke='{{ACCENT}}' stroke-width='8' "
                    + "stroke-linecap='round'/></g>"
                    + "<g class='pop'><path d='M60 250 L60 310 M35 265 L85 295 M35 295 L85 265' stroke='{{GOLD}}' stroke-width='7' "
                    + "stroke-linecap='round'/></g>"),
            Map.entry("pan", "<g class='pop'><ellipse cx='170' cy='230' rx='130' ry='60' fill='{{INK}}' opacity='.85'/>"
                    + "<ellipse cx='170' cy='222' rx='110' ry='45' fill='{{GOLD}}' opacity='.75'/></g>"
                    + "<g class='pop'><path d='M295 215 L380 185' stroke='{{INK}}' stroke-width='22' stroke-linecap='round' opacity='.85'/></g>"
                    + "<g class='pop'><path d='M120 150 Q135 120 120 90 M170 145 Q185 115 170 85 M220 150 Q235 120 220 90' stroke='{{ACCENT}}' "
                    + "stroke-width='8' fill='none' stroke-linecap='round'/></g>"),
            Map.entry("timer", "<g class='pop'><circle cx='200' cy='215' r='135' fill='#FFFFFF' stroke='" + LINE + "' stroke-width='8'/>"
                    + "<rect x='180' y='50' width='40' height='30' rx='8' fill='{{ACCENT}}'/></g>"
                    + "<g class='pop'><path d='M200 215 L200 125' stroke='{{INK}}' stroke-width='10' stroke-linecap='round'/>"
                    + "<path d='M200 215 L262 250' stroke='{{ACCENT}}' stroke-width='10' stroke-linecap='round'/>"
                    + "<circle cx='200' cy='215' r='14' fill='{{GOLD}}'/></g>"
                    + "<g class='pop'><path d='M200 215 L200 80 A135 135 0 0 1 317 148 Z' fill='{{BLUSH}}' opacity='.7'/></g>"),
            Map.entry("knife", "<g class='pop'><rect x='40' y='250' width='320' height='90' rx='22' fill='#E9C9A3' stroke='#C79A6B' stroke-width='5'/></g>"
                    + "<g class='pop'><path d='M90 220 L290 120 Q320 110 315 140 L130 245 Z' fill='#FFFFFF' stroke='" + LINE + "' stroke-width='5'/>"
                    + "<path d='M90 220 L40 245' stroke='{{INK}}' stroke-width='22' stroke-linecap='round' opacity='.85'/></g>"
                    + "<g class='pop'><circle cx='250' cy='285' r='24' fill='{{ACCENT}}'/><circle cx='305' cy='295' r='18' fill='{{GOLD}}'/>"
                    + "<circle cx='150' cy='290' r='20' fill='{{ACCENT}}' opacity='.7'/></g>"));

    private VideoArt() {
    }

    public static String hero(String decoration, int size) {
        String body = HEROES.getOrDefault(decoration, HEROES.get("cake"));
        return "<svg id='art' xmlns='http://www.w3.org/2000/svg' viewBox='0 0 600 600' width='" + size + "' height='" + size
                + "'>" + body + "</svg>";
    }

    public static String icon(String key, int size) {
        String body = ICONS.getOrDefault(key, ICONS.get("bowl"));
        return "<svg id='art' xmlns='http://www.w3.org/2000/svg' viewBox='0 0 400 400' width='" + size + "' height='" + size
                + "'>" + body + "</svg>";
    }
}
