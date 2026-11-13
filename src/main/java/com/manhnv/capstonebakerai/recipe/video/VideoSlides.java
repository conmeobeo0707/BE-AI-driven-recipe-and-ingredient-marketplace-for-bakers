package com.manhnv.capstonebakerai.recipe.video;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class VideoSlides {

    public static final int WIDTH = 1920;
    public static final int HEIGHT = 1080;

    public record Row(String name, String quantity) { }


    private static final String BASE_CSS = "html,body{margin:0;width:1920px;height:1080px;overflow:hidden;background:{{CREAM}}}"
            + ".stage{position:relative;width:1920px;height:1080px;font-family:'Nunito','Segoe UI','Noto Sans',Arial,sans-serif;color:{{INK}}}"
            + ".serif{font-family:'Playfair Display',Georgia,serif}"
            + ".blob{position:absolute;border-radius:50%;background:{{BLUSH}}}"
            + ".kicker{display:inline-block;padding:10px 26px;border-radius:40px;background:{{ACCENT}};color:#fff;font-size:22px;"
            + "font-weight:800;letter-spacing:5px}"
            + "#rule{width:120px;height:8px;border-radius:4px;background:{{GOLD}};margin-top:24px}";

    private static final String ART_JS = ".from('#plate',{scale:0,transformOrigin:'50% 50%',duration:0.6,ease:'back.out(1.6)'},0.3)"
            + ".from(gsap.utils.toArray('.pop'),{scale:0,opacity:0,transformOrigin:'50% 100%',duration:0.5,ease:'back.out(1.8)',stagger:0.1},0.5)"
            + ".from(gsap.utils.toArray('.spark'),{scale:0,rotation:-90,transformOrigin:'50% 50%',duration:0.6,ease:'back.out(2)',stagger:0.15},1.4)";

    private static final String FLAME_JS = "if(document.querySelector('#flame')){tl.to('#flame',{scaleY:1.15,scaleX:0.9,"
            + "transformOrigin:'50% 100%',duration:0.25,yoyo:true,repeat:9,ease:'sine.inOut'},1.8);}";

    public Map<String, Object> intro(VideoTheme theme, String category, String title, String description) {
        List<String> lines = splitTitle(title);
        int longest = lines.stream().mapToInt(String::length).max().orElse(0);
        int titleSize = longest <= 11 ? 124 : longest <= 15 ? 104 : longest <= 20 ? 86 : 72;
        StringBuilder h1 = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            h1.append("<span class='ln'><span class='tl").append(i == lines.size() - 1 && lines.size() > 1 ? " hl" : "")
                    .append("'>").append(esc(lines.get(i))).append("</span></span>");
        }
        String html = "<div class='stage' id='stage'><div class='blob' id='b1'></div><div class='blob' id='b2'></div>"
                + "<div class='left'><div class='kicker' id='pill'>" + esc(upper(category, "CÔNG THỨC")) + "</div>"
                + "<h1 class='serif'>" + h1 + "</h1><div id='rule'></div>"
                + (description.isBlank() ? "" : "<p id='desc'>" + esc(truncate(description, 190)) + "</p>")
                + "</div><div id='artbox'>" + VideoArt.hero(theme.decoration(), 760) + "</div></div>";
        String css = "#b1{width:900px;height:900px;right:-120px;top:90px;transform:scale(0)}"
                + "#b2{width:240px;height:240px;left:-70px;bottom:-70px;opacity:0.7;transform:scale(0)}"
                + ".left{position:absolute;left:140px;top:150px;width:900px}"
                + "h1{margin:20px 0 0;font-size:" + titleSize + "px;line-height:1;font-weight:800;letter-spacing:-2px}"
                + ".ln{display:block;overflow:hidden;padding:10px 0 4px}.ln span{display:inline-block;transform:translateY(115%)}"
                + ".hl{color:{{ACCENT}};font-style:italic}#rule{width:140px;margin:28px 0 26px;transform:scaleX(0);transform-origin:left}"
                + "p{margin:0;font-size:34px;line-height:1.5;max-width:820px;opacity:0;transform:translateY(16px);filter:blur(8px)}"
                + "#artbox{position:absolute;right:110px;top:190px;width:760px;height:760px}";
        String js = "const tl=gsap.timeline();tl.to('#b1',{scale:1,duration:0.9,ease:'power3.out'},0)"
                + ".to('#b2',{scale:1,duration:0.9,ease:'power3.out'},0.1)"
                + ".from('#pill',{opacity:0,y:20,duration:0.5,ease:'power3.out'},0.2)"
                + ".to(gsap.utils.toArray('.ln span'),{y:0,duration:0.7,ease:'power3.out',stagger:0.12},0.3)"
                + ".to('#rule',{scaleX:1,duration:0.6,ease:'power3.out'},0.9)"
                + (description.isBlank() ? "" : ".to('#desc',{opacity:1,y:0,filter:'blur(0px)',duration:0.7,ease:'power3.out'},1.0)")
                + ART_JS + ";" + FLAME_JS;
        return asset(theme, html, css, js);
    }

    public Map<String, Object> overview(VideoTheme theme, Integer prep, Integer cook, Integer servings,
                                        String difficulty, int difficultyLevel) {
        String bars = "<rect x='3' y='14' width='4.5' height='7' rx='1.2' fill='" + (difficultyLevel >= 1 ? "{{ACCENT}}" : "#EAD3C8") + "'/>"
                + "<rect x='9.75' y='9' width='4.5' height='12' rx='1.2' fill='" + (difficultyLevel >= 2 ? "{{ACCENT}}" : "#EAD3C8") + "'/>"
                + "<rect x='16.5' y='4' width='4.5' height='17' rx='1.2' fill='" + (difficultyLevel >= 3 ? "{{ACCENT}}" : "#EAD3C8") + "'/>";
        String html = "<div class='stage' id='stage'><div class='blob' id='b1'></div><div id='head'><div class='kicker' id='kick'>THÔNG TIN</div>"
                + "<div class='serif' id='title'>Tổng quan công thức</div><div id='rule'></div></div><div id='cards'>"
                + card("c1", "<circle cx='12' cy='12' r='9' fill='none' stroke='{{ACCENT}}' stroke-width='2.2'/>"
                + "<path d='M12 7v5l3 2' fill='none' stroke='{{ACCENT}}' stroke-width='2.2' stroke-linecap='round'/>",
                "CHUẨN BỊ", number(prep), "phút")
                + card("c2", "<path d='M12 3c1 3 5 5 5 10a5 5 0 0 1-10 0c0-2 1-3.5 2-4.5 0 2 1 3 2 3 0-3-1-5 1-8.5z' fill='{{ACCENT}}'/>",
                "NẤU / NƯỚNG", number(cook), "phút")
                + card("c3", "<circle cx='9' cy='8' r='3.2' fill='{{ACCENT}}'/><path d='M3 19c0-3.3 2.7-5.5 6-5.5s6 2.2 6 5.5z' fill='{{ACCENT}}'/>"
                + "<circle cx='17' cy='9' r='2.5' fill='{{ACCENT}}' opacity='.55'/>"
                + "<path d='M15.5 13.6c3 0 5.5 1.9 5.5 5.4h-4.6c0-2-0.4-3.8-0.9-5.4z' fill='{{ACCENT}}' opacity='.55'/>",
                "KHẨU PHẦN", number(servings), "người")
                + "<div class='card' id='c4'><div class='ic'><svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' width='60' height='60'>"
                + bars + "</svg></div><div class='cl'>ĐỘ KHÓ</div><div class='cv serif sm'>" + esc(difficulty) + "</div></div>"
                + "</div><div id='total'>Tổng thời gian: <b>" + (safe(prep) + safe(cook)) + " phút</b></div></div>";
        String css = "#b1{width:760px;height:760px;right:-220px;bottom:-300px;transform:scale(0)}"
                + "#head{position:absolute;left:140px;top:100px}"
                + "#title{font-size:84px;font-weight:800;letter-spacing:-1.5px;line-height:1.05;margin-top:20px}"
                + "#cards{position:absolute;left:140px;top:390px;width:1640px;display:flex;gap:40px}"
                + ".card{flex:1;height:400px;background:#fff;border-radius:36px;box-shadow:0 20px 50px rgba(59,42,35,.08);"
                + "padding:44px;box-sizing:border-box;display:flex;flex-direction:column}"
                + ".ic{width:110px;height:110px;border-radius:50%;background:{{BLUSH}};display:flex;align-items:center;justify-content:center}"
                + ".cl{font-size:24px;font-weight:800;letter-spacing:4px;opacity:.6;margin-top:44px}"
                + ".cv{font-size:100px;font-weight:800;line-height:1.1;margin-top:10px}"
                + ".cv span{font-family:'Nunito','Segoe UI',system-ui,sans-serif;font-size:34px;font-weight:700;opacity:.65;margin-left:12px}"
                + ".cv.sm{font-size:58px;margin-top:30px;color:{{ACCENT}}}"
                + "#total{position:absolute;left:140px;top:870px;font-size:36px;padding:16px 34px;border-radius:40px;background:#fff;"
                + "box-shadow:0 10px 30px rgba(59,42,35,.06)}#total b{color:{{ACCENT}}}";
        String js = "const tl=gsap.timeline();tl.to('#b1',{scale:1,duration:0.9,ease:'power3.out'},0)"
                + ".from('#kick',{opacity:0,y:20,duration:0.5,ease:'power3.out'},0.1)"
                + ".from('#title',{opacity:0,y:40,duration:0.6,ease:'power3.out'},0.2)"
                + ".from('#rule',{scaleX:0,transformOrigin:'0% 50%',duration:0.5,ease:'power3.out'},0.45)"
                + ".from(['#c1','#c2','#c3','#c4'],{opacity:0,y:60,duration:0.6,ease:'back.out(1.4)',stagger:0.12},0.5)"
                + ".from('#total',{opacity:0,y:20,duration:0.5,ease:'power3.out'},1.2);";
        return asset(theme, html, css, js);
    }

    public Map<String, Object> list(VideoTheme theme, String kicker, String title, String subtitle, List<Row> rows,
                                    String art) {
        List<Row> colA = rows.subList(0, Math.min(7, rows.size()));
        List<Row> colB = rows.size() > 7 ? rows.subList(7, rows.size()) : List.of();
        boolean wideArt = colB.isEmpty();
        StringBuilder html = new StringBuilder("<div class='stage' id='stage'><div class='blob' id='b1'></div><div id='head'>")
                .append("<div class='kicker' id='kick'>").append(esc(kicker)).append("</div><div class='serif' id='title'>")
                .append(esc(title));
        if (subtitle != null && !subtitle.isBlank()) html.append(" <span class='sm'>· ").append(esc(subtitle)).append("</span>");
        html.append("</div><div id='rule'></div></div><div id='colA'>").append(rows(colA)).append("</div>");
        if (!colB.isEmpty()) html.append("<div id='colB'>").append(rows(colB)).append("</div>");
        if (wideArt || colB.size() <= 3) html.append("<div id='artbox'>").append(art).append("</div>");
        html.append("</div>");
        String css = "#b1{width:560px;height:560px;left:1140px;top:560px;transform:scale(0)}"
                + "#head{position:absolute;left:140px;top:90px}"
                + "#title{font-size:84px;font-weight:800;letter-spacing:-1.5px;line-height:1.05;margin-top:20px}"
                + ".sm{font-family:'Nunito','Segoe UI',system-ui,sans-serif;font-size:36px;font-weight:700;color:{{GOLD}};letter-spacing:0}"
                + "#colA{position:absolute;left:140px;top:320px;width:860px}"
                + "#colB{position:absolute;left:1060px;top:320px;width:720px}"
                + ".row{display:flex;justify-content:space-between;align-items:center;height:70px;padding:0 30px;margin-bottom:12px;"
                + "background:#fff;border-radius:20px;box-shadow:0 8px 24px rgba(59,42,35,.06);font-size:32px;font-weight:700;box-sizing:border-box}"
                + ".dot{display:inline-block;width:12px;height:12px;border-radius:50%;background:{{GOLD}};margin-right:18px;vertical-align:middle}"
                + ".q{color:{{ACCENT}};font-weight:800;white-space:nowrap;margin-left:20px}"
                + ".nm{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}"
                + (wideArt ? "#artbox{position:absolute;left:1120px;top:260px;width:640px;height:640px}"
                : "#artbox{position:absolute;left:1180px;top:" + (320 + colB.size() * 82 + 20) + "px;width:480px;height:480px}");
        String js = "const tl=gsap.timeline();tl.to('#b1',{scale:1,duration:0.9,ease:'power3.out'},0.3)"
                + ".from('#kick',{opacity:0,y:20,duration:0.5,ease:'power3.out'},0.1)"
                + ".from('#title',{opacity:0,y:40,duration:0.6,ease:'power3.out'},0.2)"
                + ".from('#rule',{scaleX:0,transformOrigin:'0% 50%',duration:0.5,ease:'power3.out'},0.45)"
                + ".from(gsap.utils.toArray('.row'),{opacity:0,x:-40,duration:0.5,ease:'power3.out',stagger:0.09},0.6)"
                + ART_JS + ";";
        return asset(theme, html.toString(), css, js);
    }

    public Map<String, Object> step(VideoTheme theme, String kicker, int index, int total, String instruction,
                                    List<String> ingredients, List<String> tools, List<String> badges, String notes,
                                    String icon) {
        int length = instruction.length();
        int insSize = length <= 110 ? 46 : length <= 190 ? 40 : length <= 280 ? 34 : 30;
        StringBuilder html = new StringBuilder("<div class='stage' id='stage'><div class='blob' id='b1'></div><div id='left'>")
                .append("<div class='kicker' id='kick'>").append(esc(kicker)).append("</div>")
                .append("<div class='serif' id='num'>").append(String.format("%02d", index)).append("<span>/")
                .append(String.format("%02d", total)).append("</span></div><div id='rule'></div>")
                .append("<div id='ins'>").append(esc(truncate(instruction, 360))).append("</div>");
        if (!ingredients.isEmpty()) html.append("<div class='pl'>NGUYÊN LIỆU</div><div class='chips'>")
                .append(chips(ingredients, "ci")).append("</div>");
        if (!tools.isEmpty()) html.append("<div class='pl'>DỤNG CỤ</div><div class='chips'>")
                .append(chips(tools, "ct")).append("</div>");
        if (notes != null && !notes.isBlank()) html.append("<div id='note'><b>Lưu ý:</b> ").append(esc(truncate(notes, 140))).append("</div>");
        html.append("</div><div id='panel'><div id='artbox'>").append(VideoArt.icon(icon, 400)).append("</div>");
        if (!badges.isEmpty()) {
            html.append("<div class='badges'>");
            for (String badge : badges) html.append("<span class='badge'>").append(esc(badge)).append("</span>");
            html.append("</div>");
        }
        html.append("</div><div id='prog'>");
        for (int i = 1; i <= total; i++) {
            html.append(i < index ? "<div class='seg done'></div>" : i == index ? "<div class='seg'><i id='cur'></i></div>" : "<div class='seg'></div>");
        }
        html.append("</div></div>");
        String css = "#b1{width:700px;height:700px;left:-280px;bottom:-340px;transform:scale(0)}"
                + "#left{position:absolute;left:140px;top:90px;width:1000px}"
                + "#num{font-size:150px;font-weight:800;line-height:1;color:{{ACCENT}};margin-top:14px;letter-spacing:-4px}"
                + "#num span{font-size:52px;color:{{INK}};opacity:.3;margin-left:10px;letter-spacing:0}"
                + "#rule{margin:18px 0 28px}"
                + "#ins{font-size:" + insSize + "px;line-height:1.4;font-weight:600}"
                + ".pl{font-size:20px;font-weight:800;letter-spacing:4px;opacity:.55;margin:26px 0 12px}"
                + ".chips{display:flex;flex-wrap:wrap;gap:12px}"
                + ".chip{padding:12px 22px;border-radius:40px;font-size:26px;font-weight:700}"
                + ".ci{background:{{BLUSH}}}.ct{background:#fff;border:2px solid #EAD3C8}"
                + "#note{margin-top:24px;font-size:26px;font-style:italic;opacity:.8}#note b{color:{{ACCENT}};font-style:normal}"
                + "#panel{position:absolute;left:1220px;top:140px;width:560px;height:740px;background:#fff;border-radius:36px;"
                + "box-shadow:0 20px 50px rgba(59,42,35,.08);box-sizing:border-box;display:flex;flex-direction:column;"
                + "align-items:center;justify-content:center;gap:40px}"
                + "#artbox{width:400px;height:400px}"
                + ".badges{display:flex;flex-wrap:wrap;justify-content:center;gap:16px;padding:0 30px}"
                + ".badge{padding:12px 28px;border-radius:40px;background:{{INK}};color:#fff;font-size:28px;font-weight:800}"
                + "#prog{position:absolute;left:140px;top:965px;width:1640px;display:flex;gap:12px}"
                + ".seg{position:relative;flex:1;height:10px;border-radius:5px;background:#EAD3C8;overflow:hidden}"
                + ".seg.done{background:{{GOLD}}}"
                + ".seg i{position:absolute;left:0;top:0;width:100%;height:100%;background:{{ACCENT}}}";
        String js = "const tl=gsap.timeline();tl.to('#b1',{scale:1,duration:0.9,ease:'power3.out'},0)"
                + ".from('#kick',{opacity:0,y:20,duration:0.5,ease:'power3.out'},0.1)"
                + ".from('#num',{opacity:0,y:50,duration:0.6,ease:'back.out(1.4)'},0.2)"
                + ".from('#rule',{scaleX:0,transformOrigin:'0% 50%',duration:0.5,ease:'power3.out'},0.45)"
                + ".from('#ins',{opacity:0,y:16,filter:'blur(8px)',duration:0.7,ease:'power3.out'},0.55)"
                + ".from(gsap.utils.toArray('.pl'),{opacity:0,duration:0.4},0.9)"
                + ".from(gsap.utils.toArray('.chip'),{opacity:0,scale:0.8,duration:0.4,ease:'back.out(2)',stagger:0.07},1.0)"
                + ".from('#panel',{opacity:0,x:60,duration:0.6,ease:'power3.out'},0.4)"
                + ".from(gsap.utils.toArray('.pop'),{scale:0,opacity:0,transformOrigin:'50% 50%',duration:0.5,ease:'back.out(1.8)',stagger:0.12},0.8)"
                + ".from(gsap.utils.toArray('.badge'),{opacity:0,y:20,duration:0.5,ease:'back.out(2)',stagger:0.1},1.2)"
                + ".from('#cur',{scaleX:0,transformOrigin:'0% 50%',duration:0.8,ease:'power3.out'},0.5)"
                + (notes != null && !notes.isBlank() ? ".from('#note',{opacity:0,y:12,duration:0.5},1.4)" : "") + ";";
        return asset(theme, html.toString(), css, js);
    }

    public Map<String, Object> outro(VideoTheme theme, String title) {
        String html = "<div class='stage' id='stage'><div class='blob' id='b1'></div><div class='blob' id='b2'></div>"
                + "<div class='left'><div class='kicker' id='pill'>HOÀN THÀNH</div>"
                + "<h1 class='serif'><span class='ln'><span>Chúc</span></span><span class='ln'><span class='hl'>ngon miệng!</span></span></h1>"
                + "<div id='rule'></div><p id='desc'>" + esc(truncate(title, 80)) + "</p></div>"
                + "<div id='artbox'>" + VideoArt.hero(theme.decoration(), 700) + "</div></div>";
        String css = "#b1{width:860px;height:860px;right:-100px;top:110px;transform:scale(0)}"
                + "#b2{width:260px;height:260px;left:-80px;top:-80px;opacity:0.7;transform:scale(0)}"
                + ".left{position:absolute;left:140px;top:240px;width:900px}"
                + "h1{margin:20px 0 0;font-size:130px;line-height:1;font-weight:800;letter-spacing:-2px}"
                + ".ln{display:block;overflow:hidden;padding:10px 0 4px}.ln span{display:inline-block;transform:translateY(115%)}"
                + ".hl{color:{{ACCENT}};font-style:italic}#rule{width:140px;margin:28px 0 26px;transform:scaleX(0);transform-origin:left}"
                + "p{margin:0;font-size:40px;font-weight:700;opacity:0}"
                + "#artbox{position:absolute;right:140px;top:220px;width:700px;height:700px}";
        String js = "const tl=gsap.timeline();tl.to('#b1',{scale:1,duration:0.9,ease:'power3.out'},0)"
                + ".to('#b2',{scale:1,duration:0.9,ease:'power3.out'},0.1)"
                + ".from('#pill',{opacity:0,y:20,duration:0.5,ease:'power3.out'},0.2)"
                + ".to(gsap.utils.toArray('.ln span'),{y:0,duration:0.7,ease:'power3.out',stagger:0.15},0.3)"
                + ".to('#rule',{scaleX:1,duration:0.6,ease:'power3.out'},0.9)"
                + ".to('#desc',{opacity:1,duration:0.6},1.0)" + ART_JS + ";" + FLAME_JS;
        return asset(theme, html, css, js);
    }

    private Map<String, Object> asset(VideoTheme theme, String html, String css, String js) {
        Map<String, Object> asset = new LinkedHashMap<>();
        asset.put("type", "html5");
        asset.put("html", colors(theme, html));
        asset.put("css", colors(theme, BASE_CSS + css));
        asset.put("js", js);
        return asset;
    }

    private static String card(String id, String icon, String label, String value, String unit) {
        return "<div class='card' id='" + id + "'><div class='ic'><svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' "
                + "width='60' height='60'>" + icon + "</svg></div><div class='cl'>" + label + "</div><div class='cv serif'>"
                + value + "<span>" + unit + "</span></div></div>";
    }

    private static String rows(List<Row> rows) {
        StringBuilder out = new StringBuilder();
        for (Row row : rows) {
            out.append("<div class='row'><span class='nm'><i class='dot'></i>").append(esc(truncate(row.name(), 34)))
                    .append("</span><span class='q'>").append(esc(row.quantity())).append("</span></div>");
        }
        return out.toString();
    }

    private static String chips(List<String> items, String type) {
        StringBuilder out = new StringBuilder();
        for (String item : items.subList(0, Math.min(items.size(), 6))) {
            out.append("<span class='chip ").append(type).append("'>").append(esc(truncate(item, 40))).append("</span>");
        }
        return out.toString();
    }

    private static List<String> splitTitle(String title) {
        String[] words = title.trim().split("\\s+");
        int lineCount = words.length <= 2 ? 1 : words.length <= 5 ? 2 : 3;
        int target = (int) Math.ceil((double) title.length() / lineCount);
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            if (!current.isEmpty() && current.length() + word.length() > target && lines.size() < lineCount - 1) {
                lines.add(current.toString());
                current.setLength(0);
            }
            if (!current.isEmpty()) current.append(' ');
            current.append(word);
        }
        if (!current.isEmpty()) lines.add(current.toString());
        return lines;
    }

    private static String colors(VideoTheme theme, String text) {
        return text.replace("{{CREAM}}", theme.cream()).replace("{{BLUSH}}", theme.blush())
                .replace("{{ACCENT}}", theme.accent()).replace("{{GOLD}}", theme.gold()).replace("{{INK}}", theme.ink());
    }

    private static String number(Integer value) {
        return value == null ? "–" : String.valueOf(value);
    }

    private static int safe(Integer value) {
        return value == null ? 0 : value;
    }

    private static String upper(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim().toUpperCase(java.util.Locale.forLanguageTag("vi"));
    }

    private static String truncate(String value, int max) {
        if (value == null) return "";
        String trimmed = value.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max - 1) + "…";
    }

    public static String esc(String value) {
        if (value == null) return "";
        StringBuilder out = new StringBuilder(value.length());
        for (char c : value.toCharArray()) {
            switch (c) {
                case '&' -> out.append("&amp;");
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '"' -> out.append("&quot;");
                case '\'' -> out.append("&#39;");
                default -> out.append(c);
            }
        }
        return out.toString();
    }
}
