package com.manhnv.capstonebakerai.recipe.video;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Component
public class VideoThemeCatalog {

    record Palette(String cream, String blush, String accent, String gold, String ink) { }

    private static final Map<String, Palette> PALETTES = new LinkedHashMap<>();
    private static final List<String> DECORATIONS = List.of("cake", "cupcake", "bread", "donut", "cookie", "macaron");
    private static final List<String> STEP_ICONS =
            List.of("oven", "bowl", "sieve", "piping", "rolling", "fridge", "pan", "timer", "knife");

    static {
        PALETTES.put("strawberry", new Palette("#FFF7F2", "#FBE0DA", "#E5576E", "#E8A93B", "#3B2A23"));
        PALETTES.put("chocolate", new Palette("#FBF4EC", "#EEDCCB", "#7A4A2E", "#D49A3A", "#2E1F18"));
        PALETTES.put("matcha", new Palette("#F6FAF1", "#DCEBD2", "#4F8A3F", "#D9A84A", "#22301F"));
        PALETTES.put("blueberry", new Palette("#F5F6FD", "#DDE2F7", "#4F5FC6", "#E3A945", "#23284A"));
        PALETTES.put("caramel", new Palette("#FFF8EE", "#F8E1C2", "#C9772B", "#8E5A2B", "#3A2716"));
        PALETTES.put("lemon", new Palette("#FFFCEB", "#FDF0B5", "#D79A12", "#4FA3A5", "#33301A"));
        PALETTES.put("lavender", new Palette("#FAF6FD", "#EADFF6", "#8B5FBF", "#E3A945", "#2E2440"));
        PALETTES.put("mint", new Palette("#F2FBF8", "#D3F0E6", "#2E9C7E", "#F08A6C", "#1E3530"));
    }

    public List<String> palettes() {
        return List.copyOf(PALETTES.keySet());
    }

    public List<String> decorations() {
        return DECORATIONS;
    }

    public List<String> stepIcons() {
        return STEP_ICONS;
    }

    public VideoTheme fallback(UUID recipeId, String title, String category) {
        String text = normalize((category == null ? "" : category) + " " + (title == null ? "" : title));
        int seed = recipeId == null ? text.hashCode() : recipeId.hashCode();
        return build(guessPalette(text, seed), guessDecoration(text, seed));
    }

    public VideoTheme resolve(String palette, String decoration, VideoTheme fallback) {
        return build(PALETTES.containsKey(palette) ? palette : fallback.palette(),
                DECORATIONS.contains(decoration) ? decoration : fallback.decoration());
    }

    public String validIcon(String icon) {
        return STEP_ICONS.contains(icon) ? icon : null;
    }

    public String guessIcon(String instruction) {
        String text = normalize(instruction == null ? "" : instruction);
        if (text.matches(".*(lo nuong|nuong|bake|oven|lam nong lo).*")) return "oven";
        if (text.matches(".*(ray|sieve|sang).*")) return "sieve";
        if (text.matches(".*(tu lanh|lam lanh|dong lanh|fridge|chill|lam mat).*")) return "fridge";
        if (text.matches(".*(ban kem|tui ban|piping|trang tri|phet kem|phu kem).*")) return "piping";
        if (text.matches(".*(can bot|can mong|lan bot|roll|nhao|nhoi).*")) return "rolling";
        if (text.matches(".*(cat|thai|bam|knife|chop|slice).*")) return "knife";
        if (text.matches(".*(chao|dun|nau|chien|xao|pan|dun soi|dun nong).*")) return "pan";
        if (text.matches(".*(u bot|nghi|cho|de yen|rest|proof).*")) return "timer";
        return "bowl";
    }

    private VideoTheme build(String paletteKey, String decoration) {
        Palette p = PALETTES.get(paletteKey);
        return new VideoTheme(paletteKey, decoration, p.cream(), p.blush(), p.accent(), p.gold(), p.ink());
    }

    private String guessPalette(String text, int seed) {
        if (text.matches(".*(dau tay|strawberry|red velvet|hong).*")) return "strawberry";
        if (text.matches(".*(socola|chocolate|cacao|ca phe|coffee|tiramisu|brownie).*")) return "chocolate";
        if (text.matches(".*(matcha|tra xanh|la dua|pandan).*")) return "matcha";
        if (text.matches(".*(viet quat|blueberry|khoai mon|ube).*")) return "blueberry";
        if (text.matches(".*(caramel|flan|banh mi|bread|croissant|mat ong|honey).*")) return "caramel";
        if (text.matches(".*(chanh|lemon|xoai|mango|chanh day).*")) return "lemon";
        if (text.matches(".*(lavender|oai huong|nho|grape).*")) return "lavender";
        if (text.matches(".*(bac ha|mint|dua|coconut).*")) return "mint";
        return palettes().get(Math.floorMod(seed, PALETTES.size()));
    }

    private String guessDecoration(String text, int seed) {
        if (text.matches(".*(cupcake|muffin).*")) return "cupcake";
        if (text.matches(".*(macaron|meringue).*")) return "macaron";
        if (text.matches(".*(banh mi|bread|croissant|sandwich|baguette|brioche).*")) return "bread";
        if (text.matches(".*(cookie|quy|biscuit).*")) return "cookie";
        if (text.matches(".*(donut|doughnut|banh vong).*")) return "donut";
        if (text.matches(".*(banh kem|cake|gato|sinh nhat|mousse|cheesecake|bong lan|tiramisu).*")) return "cake";
        return DECORATIONS.get(Math.floorMod(seed >>> 4, DECORATIONS.size()));
    }

    private static String normalize(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").replace('đ', 'd').replace('Đ', 'D').toLowerCase(Locale.ROOT);
    }
}
