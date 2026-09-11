package rwg.world;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.biome.BiomeGenBase;

import com.google.common.base.Optional;

import one.profiler.AsyncProfiler;
import rwg.biomes.base.BaseBiomes;
import rwg.biomes.realistic.RealisticBiomeBase;
import rwg.biomes.realistic.land.RealisticBiomeMountainChain;
import rwg.biomes.realistic.ocean.RealisticBiomeOcean;
import rwg.config.ConfigRWG;
import rwg.support.RealisticBiomeSupport;
import rwg.support.Support;
import rwg.support.Support.BiomePlacement;
import rwg.support.SupportBOP;
import rwg.support.SupportEBXL;
import rwg.support.SupportTC;

/** Offline world-generation preview used by {@code runPreview} and {@code runPerfCheck}. */
public final class RWGPreviewTool {

    private static final int SIZE = 40960;
    private static final int BLOCKS_PER_PIXEL = 16;
    private static final int RESOLUTION = SIZE / BLOCKS_PER_PIXEL;
    private static final int PIXELS_PER_CHUNK = 16 / BLOCKS_PER_PIXEL;
    private static final int CHUNKS = SIZE / 16;
    private static final int GRID = 4;
    private static final int WORKERS = GRID * GRID;
    private static final long SEED = 927692613931855800L;
    private static final int[] CATEGORY_COLORS = { 0x287EB3, 0x91B69C, 0x447A52, 0xD2B44B, 0x3C9166, 0x916FA3, 0xD58A45,
            0x666666 };
    private static final List<BiomeGenBase> ADDON_BIOMES = new ArrayList<BiomeGenBase>();
    private static final Map<BiomeGenBase, String> ADDON_SOURCES = new IdentityHashMap<BiomeGenBase, String>();
    private static final Map<BiomeGenBase, String> ADDON_COMMENTS = new IdentityHashMap<BiomeGenBase, String>();

    public static void main(String[] args) {
        boolean instrumentOnly = Arrays.asList(args).contains("--instrument");
        File configFile = previewConfig(args);
        ConfigRWG.init(configFile);
        System.out.println("RWG preview config: " + configFile.getAbsolutePath());
        BaseBiomes.load();
        Support.init(false);
        initAddonStubs();
        SupportBOP.init();
        SupportEBXL.init();
        SupportTC.init();
        Support.rebuildExtremeBorderMountains();
        open(instrumentOnly);
    }

    private static File previewConfig(String[] args) {
        for (String argument : args) {
            if (argument.startsWith("--config=")) return new File(argument.substring("--config=".length()));
        }
        return new File(System.getProperty("rwg.previewConfig", "run/client/config/RWG.cfg"));
    }

    private static void initAddonStubs() {
        try {
            for (Field field : Class.forName("biomesoplenty.api.content.BOPCBiomes").getFields()) {
                if (Modifier.isStatic(field.getModifiers()) && field.getType() == BiomeGenBase.class) {
                    field.set(null, debugBiome(field.getName(), "BOP"));
                }
            }
            for (Field field : Class.forName("biomesoplenty.api.content.BOPCBlocks").getFields()) {
                if (Modifier.isStatic(field.getModifiers()) && field.getType() == Block.class) {
                    field.set(null, field.getName().contains("ash") ? Blocks.sand : Blocks.stone);
                }
            }
            for (Field field : Class.forName("extrabiomes.api.BiomeManager").getFields()) {
                if (Modifier.isStatic(field.getModifiers()) && field.getType() == Optional.class) {
                    field.set(null, Optional.of(debugBiome(field.getName(), "EBXL")));
                }
            }
            debugBiome("taintedLand", "Thaumcraft");
            debugBiome("magicalForest", "Thaumcraft");
        } catch (ReflectiveOperationException exception) {
            throw new RuntimeException("Could not create offline addon biome stubs", exception);
        }
    }

    private static BiomeGenBase debugBiome(String fieldName, String source) {
        String lower = fieldName.toLowerCase();
        float temperature = lower.matches(".*(alps|arctic|boreal|frost|glacier|ice|snow|taiga|tundra).*") ? .2f
                : lower.matches(".*(bamboo|bayou|desert|jungle|lush|oasis|outback|rain|savanna|tropic|volcano).*")
                        ? 1.2f
                        : .7f;
        BiomeGenBase biome = new DebugBiome(nextBiomeId()).setBiomeName(prettyName(fieldName))
                .setTemperatureRainfall(temperature, .5f);
        ADDON_BIOMES.add(biome);
        ADDON_SOURCES.put(biome, source);
        String comment = disabledComment(fieldName, source);
        if (comment != null) ADDON_COMMENTS.put(biome, comment);
        return biome;
    }

    private static String disabledComment(String fieldName, String source) {
        String lower = fieldName.toLowerCase();
        if ("BOP".equals(source) && (lower.equals("undergarden") || lower.equals("phantasmagoricinferno")
                || lower.equals("boneyard")
                || lower.equals("visceralheap")
                || lower.equals("polarchasm")
                || lower.equals("spectralgarden")))
            return "Nether biome";
        if (lower.startsWith("alps") || lower.equals("arctic")
                || lower.equals("canyonravine")
                || lower.equals("glacier")
                || lower.equals("denseforest")
                || lower.equals("sprucewoods")
                || lower.contains("river")
                || lower.contains("mountain"))
            return "Not Useful";
        if (lower.equals("xericshrubland") || lower.equals("originvalley")
                || lower.equals("silkglades")
                || lower.equals("silkglade")
                || lower.equals("mysticgrove"))
            return "Weird";
        if (lower.equals("marsh")) return "Buggy";
        return null;
    }

    private static int nextBiomeId() {
        BiomeGenBase[] biomes = BiomeGenBase.getBiomeGenArray();
        for (int id = biomes.length - 1; id >= 0; id--) if (biomes[id] == null) return id;
        throw new IllegalStateException("No free biome ID for offline addon stub");
    }

    private static String prettyName(String name) {
        return name.substring(0, 1).toUpperCase() + name.substring(1).replaceAll("([A-Z])", " $1");
    }

    private static final class DebugBiome extends BiomeGenBase {

        private DebugBiome(int id) {
            super(id);
        }
    }

    public static void open(boolean instrumentOnly) {
        ChunkManagerRealistic categoryManager = new ChunkManagerRealistic(SEED, true);
        List<RealisticBiomeBase> configuredBiomes = categoryManager.getConfiguredBiomes();
        float[] height = new float[RESOLUTION * RESOLUTION];
        byte[] biomes = new byte[RESOLUTION * RESOLUTION];
        byte[] metaBiomes = new byte[RESOLUTION * RESOLUTION];
        byte[] placements = new byte[RESOLUTION * RESOLUTION];
        BufferedImage image = new BufferedImage(RESOLUTION, RESOLUTION, BufferedImage.TYPE_INT_RGB);
        int[] pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        HighlightState highlight = new HighlightState();
        MapPanel mapPanel = instrumentOnly ? null
                : new MapPanel(image, height, biomes, metaBiomes, placements, highlight);
        JPanel root = instrumentOnly ? null : new JPanel(new BorderLayout());
        JLabel progress = instrumentOnly ? null : new JLabel("Generating world: 0%", JLabel.CENTER);
        if (!instrumentOnly) createPreviewFrame(root, mapPanel, progress);
        AtomicInteger completedChunks = new AtomicInteger();
        ExecutorService workers = Executors.newFixedThreadPool(WORKERS);
        CompletableFuture<?>[] jobs = new CompletableFuture[WORKERS];

        try {
            for (int tile = 0; tile < WORKERS; tile++) {
                final int tx = tile % GRID, tz = tile / GRID;
                jobs[tile] = CompletableFuture.runAsync(() -> {
                    ChunkManagerRealistic manager = new ChunkManagerRealistic(SEED, true);
                    ChunkGeneratorRealistic generator = new ChunkGeneratorRealistic(manager, SEED, true);
                    RealisticBiomeBase[] chunkBiomes = new RealisticBiomeBase[256];
                    for (int cz = tz * CHUNKS / GRID; cz < (tz + 1) * CHUNKS / GRID; cz++) {
                        for (int cx = tx * CHUNKS / GRID; cx < (tx + 1) * CHUNKS / GRID; cx++) {
                            float[] chunkHeight = generator.getNewNoise(
                                    manager,
                                    cx * 16 - SIZE / 2,
                                    cz * 16 - SIZE / 2,
                                    chunkBiomes,
                                    BLOCKS_PER_PIXEL);
                            for (int z = 0; z < 16; z += BLOCKS_PER_PIXEL) {
                                int target = (cz * PIXELS_PER_CHUNK + z / BLOCKS_PER_PIXEL) * RESOLUTION
                                        + cx * PIXELS_PER_CHUNK;
                                for (int x = 0; x < 16; x += BLOCKS_PER_PIXEL) {
                                    int index = target + x / BLOCKS_PER_PIXEL;
                                    int worldX = cx * 16 + x - SIZE / 2;
                                    int worldZ = cz * 16 + z - SIZE / 2;
                                    RealisticBiomeBase biome = chunkBiomes[x * 16 + z];
                                    height[index] = chunkHeight[x * 16 + z];
                                    biomes[index] = (byte) biome.biomeID;
                                    int metaBiome = manager.getMetaBiomeAt(worldX, worldZ);
                                    metaBiomes[index] = (byte) metaBiome;
                                    placements[index] = (byte) manager.getPlacementAt(metaBiome, biome);
                                    if (!instrumentOnly) pixels[index] = biomeColor(biome.biomeID, metaBiome);
                                }
                            }
                            int completed = completedChunks.incrementAndGet();
                            if (!instrumentOnly && (completed & 1023) == 0) {
                                SwingUtilities.invokeLater(() -> {
                                    progress.setText(
                                            String.format(
                                                    "Generating world: %.1f%%",
                                                    completed * 100d / (CHUNKS * CHUNKS)));
                                    mapPanel.repaint();
                                });
                            }
                        }
                    }
                }, workers);
            }
            CompletableFuture.allOf(jobs).join();
            if (instrumentOnly) {
                stopAsyncProfiler();
                return;
            }
            SwingUtilities.invokeLater(() -> {
                progress.setText("Lighting preview…");
                mapPanel.repaint();
            });
            for (int tile = 0; tile < WORKERS; tile++) {
                final int tx = tile % GRID, tz = tile / GRID;
                jobs[tile] = CompletableFuture.runAsync(() -> {
                    lightTile(
                            height,
                            biomes,
                            metaBiomes,
                            pixels,
                            tx * RESOLUTION / GRID,
                            (tx + 1) * RESOLUTION / GRID,
                            tz * RESOLUTION / GRID,
                            (tz + 1) * RESOLUTION / GRID);
                }, workers);
            }
            CompletableFuture.allOf(jobs).join();
        } finally {
            workers.shutdownNow();
        }

        JPanel sidebar = createSidebar(biomes, metaBiomes, placements, configuredBiomes, categoryManager, highlight);
        SwingUtilities.invokeLater(() -> {
            mapPanel.finishGeneration();
            root.remove(progress);
            root.add(sidebar, BorderLayout.EAST);
            root.revalidate();
            mapPanel.repaint();
        });
    }

    private static JFrame createPreviewFrame(JPanel root, MapPanel mapPanel, JLabel progress) {
        JFrame frame = new JFrame("RWG_CONTINENT — seed " + SEED);
        root.add(mapPanel);
        root.add(progress, BorderLayout.SOUTH);
        frame.add(root);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(1400, 900);
        frame.setLocationRelativeTo(null);
        frame.getRootPane().registerKeyboardAction(
                event -> frame.dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        frame.setVisible(true);
        frame.addWindowListener(new java.awt.event.WindowAdapter() {

            @Override
            public void windowClosed(java.awt.event.WindowEvent event) {
                System.exit(0);
            }
        });
        return frame;
    }

    private static void stopAsyncProfiler() {
        try {
            String output = System.getProperty("rwg.asyncProfilerOutput");
            if (output == null) throw new IllegalStateException("Missing async-profiler output path");
            AsyncProfiler.getInstance().execute("stop,file=" + output + ",title=RWG preview world generation");
            System.out.println("RWG generation workers finished; async-profiler stopped.");
        } catch (Exception exception) {
            throw new RuntimeException("Could not stop async-profiler after world generation", exception);
        }
    }

    private static JPanel createSidebar(byte[] biomes, byte[] metaBiomes, byte[] placements,
            List<RealisticBiomeBase> configuredBiomes, ChunkManagerRealistic manager, HighlightState highlight) {
        int[][][] counts = new int[5][BiomePlacement.values().length][256];
        int[] metaCounts = new int[5];
        for (int index = 0; index < biomes.length; index++) {
            int meta = metaBiomes[index] & 255;
            int placement = placements[index] & 255;
            counts[meta][placement][biomes[index] & 255]++;
            metaCounts[meta]++;
        }
        Set<BiomeGenBase> supported = new HashSet<BiomeGenBase>();
        for (RealisticBiomeBase biome : configuredBiomes) if (biome != null) supported.add(biome.baseBiome);
        List<BiomeGenBase> missing = new ArrayList<BiomeGenBase>();
        for (BiomeGenBase biome : ADDON_BIOMES) if (!supported.contains(biome)) missing.add(biome);
        Collections.sort(missing, (a, b) -> {
            String aComment = ADDON_COMMENTS.get(a);
            String bComment = ADDON_COMMENTS.get(b);
            if (aComment == null && bComment != null) return 1;
            if (aComment != null && bComment == null) return -1;
            if (aComment != null) {
                int commentOrder = aComment.compareToIgnoreCase(bComment);
                if (commentOrder != 0) return commentOrder;
            }
            return a.biomeName.compareToIgnoreCase(b.biomeName);
        });

        JPanel entries = new JPanel();
        entries.setLayout(new BoxLayout(entries, BoxLayout.Y_AXIS));
        String[] metaNames = { "Water", "Snow", "Cold", "Hot", "Wet" };
        for (int meta = 0; meta < metaNames.length; meta++) {
            if (metaCounts[meta] == 0) continue;
            List<Integer>[] idsByPlacement = configuredIds(meta, counts[meta], configuredBiomes, manager);
            int biomeCount = uniqueCount(idsByPlacement);
            double metaPercent = metaCounts[meta] * 100d / biomes.length;
            JLabel header = sidebarLabel(
                    entries,
                    String.format(
                            "%.2f%% - %s - %d biomes, avg %.2f%% per biome",
                            metaPercent,
                            metaNames[meta],
                            biomeCount,
                            biomeCount == 0 ? 0d : metaPercent / biomeCount),
                    mix(CATEGORY_COLORS[meta], 0xFFFFFF, .72f));
            addGroupHover(header, -1, meta, -1, highlight);

            int nonEmptyPlacements = 0;
            for (List<Integer> ids : idsByPlacement) if (!ids.isEmpty()) nonEmptyPlacements++;
            for (BiomePlacement placement : BiomePlacement.values()) {
                int placementIndex = placement.ordinal();
                List<Integer> ids = idsByPlacement[placementIndex];
                if (ids.isEmpty()) continue;
                if (nonEmptyPlacements > 1 && placement != BiomePlacement.CORE) {
                    JLabel placementHeader = sidebarLabel(
                            entries,
                            placementName(placement),
                            mix(CATEGORY_COLORS[meta], 0xFFFFFF, .55f));
                    addGroupHover(placementHeader, -1, meta, placementIndex, highlight);
                }
                for (int id : ids) {
                    int count = counts[meta][placementIndex][id];
                    addSidebarEntry(
                            entries,
                            String.format(
                                    "%.2f%% - %s",
                                    count * 100d / biomes.length,
                                    biomeName(RealisticBiomeBase.getBiome(id))),
                            meta == 0 ? 0x287EB3 : biomeColor(id, meta),
                            id,
                            meta,
                            placementIndex,
                            highlight);
                }
            }
        }

        JLabel disabledHeader = new JLabel("  N/A - Disabled");
        disabledHeader.setAlignmentX(0f);
        entries.add(disabledHeader);
        for (BiomeGenBase biome : missing) {
            String comment = ADDON_COMMENTS.get(biome);
            addSidebarEntry(
                    entries,
                    (comment == null ? "N/A" : comment) + " - " + biome.biomeName + sourceSuffix(biome),
                    CATEGORY_COLORS[7],
                    -1,
                    -1,
                    -1,
                    highlight);
        }

        JScrollPane content = new JScrollPane(entries);
        content.setPreferredSize(new Dimension(420, 1));
        highlight.attachSidebar(content);
        JPanel sidebar = new JPanel(new BorderLayout());
        JButton toggle = new JButton("▶");
        toggle.addActionListener(event -> {
            content.setVisible(!content.isVisible());
            toggle.setText(content.isVisible() ? "▶" : "◀");
            sidebar.revalidate();
        });
        sidebar.add(toggle, BorderLayout.WEST);
        sidebar.add(content);
        return sidebar;
    }

    @SuppressWarnings("unchecked")
    private static List<Integer>[] configuredIds(int meta, int[][] counts, List<RealisticBiomeBase> configured,
            ChunkManagerRealistic manager) {
        List<Integer>[] result = new List[BiomePlacement.values().length];
        for (int placement = 0; placement < result.length; placement++) result[placement] = new ArrayList<Integer>();
        if (meta == 0) {
            int[] categories = manager.getConfiguredBiomeCategories();
            for (RealisticBiomeBase biome : configured) {
                if (biome != null && categories[biome.biomeID] == 0)
                    result[BiomePlacement.CORE.ordinal()].add(biome.biomeID);
            }
        } else {
            for (BiomePlacement placement : BiomePlacement.values()) {
                for (RealisticBiomeBase biome : manager.getBiomesFor(meta, placement)) {
                    if (!result[placement.ordinal()].contains(biome.biomeID))
                        result[placement.ordinal()].add(biome.biomeID);
                }
            }
        }
        for (int placement = 0; placement < result.length; placement++) {
            final int group = placement;
            Collections.sort(
                    result[placement],
                    (first, second) -> Integer.compare(counts[group][second], counts[group][first]));
        }
        return result;
    }

    private static int uniqueCount(List<Integer>[] groups) {
        Set<Integer> unique = new HashSet<Integer>();
        for (List<Integer> group : groups) unique.addAll(group);
        return unique.size();
    }

    private static String placementName(BiomePlacement placement) {
        if (placement == BiomePlacement.SMALL_ISLAND) return "Small Islands";
        if (placement == BiomePlacement.LARGE_ISLAND) return "Large Islands";
        if (placement == BiomePlacement.ISLAND) return "All Island Sizes";
        StringBuilder name = new StringBuilder();
        for (String word : placement.name().toLowerCase().split("_")) {
            if (name.length() > 0) name.append(' ');
            name.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return name.toString();
    }

    private static JLabel sidebarLabel(JPanel entries, String text, int color) {
        JLabel entry = new JLabel("  " + text + "  ");
        entry.setOpaque(true);
        entry.setBackground(new Color(color));
        entry.setMaximumSize(new Dimension(Integer.MAX_VALUE, entry.getPreferredSize().height + 6));
        entry.setAlignmentX(0f);
        entries.add(entry);
        return entry;
    }

    private static void addGroupHover(JLabel entry, int biomeId, int meta, int placement, HighlightState highlight) {
        entry.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseEntered(MouseEvent event) {
                highlight.set(biomeId, meta, placement, false);
            }

            @Override
            public void mouseExited(MouseEvent event) {
                highlight.clear();
            }
        });
    }

    private static void addSidebarEntry(JPanel entries, String text, int color, int biomeId, int meta, int placement,
            HighlightState highlight) {
        JLabel entry = sidebarLabel(entries, text, color);
        if (biomeId >= 0) {
            highlight.register(biomeId, meta, placement, entry);
            entry.addMouseListener(new MouseAdapter() {

                @Override
                public void mouseEntered(MouseEvent event) {
                    highlight.set(biomeId, meta, placement, false);
                }

                @Override
                public void mouseExited(MouseEvent event) {
                    highlight.clear();
                }
            });
        }
    }

    private static String sourceSuffix(BiomeGenBase biome) {
        String source = ADDON_SOURCES.get(biome);
        return source == null ? "" : " (" + source + ")";
    }

    private static String biomeName(RealisticBiomeBase biome) {
        String name = biome.getDisplayName() == null ? biome.baseBiome.biomeName : biome.getDisplayName();
        if (biome instanceof RealisticBiomeSupport) {
            String source = ADDON_SOURCES.get(biome.baseBiome);
            if (source != null) {
                String terrain = ((RealisticBiomeSupport) biome).terrain.getClass().getSimpleName()
                        .replaceFirst("^Terrain", "").replaceAll("([a-z])([A-Z])", "$1 $2");
                return name + " (" + source + ", " + terrain + ")";
            }
            return name + " (" + biome.getClass().getSimpleName() + ")";
        }
        if (biome instanceof RealisticBiomeMountainChain) {
            String source = sourceSuffix(biome.baseBiome);
            return name + (source.isEmpty() ? " (Mountain Chain)"
                    : source.substring(0, source.length() - 1) + ", Mountain Chain)");
        }
        if (biome instanceof RealisticBiomeOcean) {
            String source = sourceSuffix(biome.baseBiome);
            String variant = ((RealisticBiomeOcean) biome).getVariantName();
            return biome.baseBiome.biomeName + (source.isEmpty() ? " (" + variant + ")"
                    : source.substring(0, source.length() - 1) + ", " + variant + ")");
        }
        return name + " (" + biome.getClass().getSimpleName() + ")";
    }

    private static void lightTile(float[] height, byte[] biomes, byte[] metaBiomes, int[] pixels, int x0, int x1,
            int z0, int z1) {
        for (int z = z0; z < z1; z++) {
            for (int x = x0; x < x1; x++) {
                int i = z * RESOLUTION + x;
                float h = height[i];
                int radius = 3;
                float dx = height[z * RESOLUTION + Math.min(x + radius, RESOLUTION - 1)]
                        - height[z * RESOLUTION + Math.max(x - radius, 0)];
                float dz = height[Math.min(z + radius, RESOLUTION - 1) * RESOLUTION + x]
                        - height[Math.max(z - radius, 0) * RESOLUTION + x];
                float light = .96f + .08f * (12f + dx + dz) / (float) Math.sqrt(dx * dx + dz * dz + 432f);
                if (h >= 63f) light += clamp((h - 63f) / 120f, 0f, 1f) * .14f;
                int id = biomes[i] & 255;
                int meta = metaBiomes[i] & 255;
                if (meta == 0) {
                    pixels[i] = biomeColor(id, meta);
                    continue;
                }
                int color = h < 63f ? mix(0x071F4A, 0x4DA6D8, clamp((h - 30f) / 33f, 0f, 1f)) : biomeColor(id, meta);
                pixels[i] = shade(color, clamp(light, .88f, 1.18f));
            }
        }
    }

    private static int biomeColor(int id, int meta) {
        return shade(CATEGORY_COLORS[meta], .92f + (id % 5) * .02f);
    }

    private static String categoryName(int meta) {
        return new String[] { "Water", "Snow", "Cold", "Hot", "Wet" }[meta];
    }

    private static int shade(int rgb, float amount) {
        return Math.min(255, (int) (((rgb >> 16) & 255) * amount)) << 16
                | Math.min(255, (int) (((rgb >> 8) & 255) * amount)) << 8
                | Math.min(255, (int) ((rgb & 255) * amount));
    }

    private static int mix(int from, int to, float amount) {
        int r = (int) (((from >> 16) & 255) * (1 - amount) + ((to >> 16) & 255) * amount);
        int g = (int) (((from >> 8) & 255) * (1 - amount) + ((to >> 8) & 255) * amount);
        int b = (int) ((from & 255) * (1 - amount) + (to & 255) * amount);
        return r << 16 | g << 8 | b;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final class HighlightState {

        private MapPanel map;
        private JScrollPane sidebar;
        private final JLabel[][][] entries = new JLabel[5][BiomePlacement.values().length][256];
        private JLabel selected;
        private int selectedBiome = -1;
        private int selectedMeta = -1;
        private int selectedPlacement = -1;

        private void attachSidebar(JScrollPane sidebar) {
            this.sidebar = sidebar;
        }

        private void register(int biomeId, int meta, int placement, JLabel entry) {
            entries[meta][placement][biomeId] = entry;
        }

        private void set(int biomeId, int meta, int placement, boolean scroll) {
            if (map != null) map.setHighlight(biomeId, meta, placement);
            if (selectedBiome == biomeId && selectedMeta == meta && selectedPlacement == placement) return;
            selectedBiome = biomeId;
            selectedMeta = meta;
            selectedPlacement = placement;
            if (selected != null) {
                selected.setBorder(null);
                selected = null;
            }
            if (biomeId >= 0 && meta >= 0 && placement >= 0 && entries[meta][placement][biomeId] != null) {
                selected = entries[meta][placement][biomeId];
                selected.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
                if (scroll && sidebar != null) selected.scrollRectToVisible(selected.getVisibleRect());
            }
        }

        private void clear() {
            set(-1, -1, -1, false);
        }
    }

    private static final class MapPanel extends JPanel {

        private final BufferedImage image;
        private final float[] height;
        private final byte[] biomes;
        private final byte[] metaBiomes;
        private final byte[] placements;
        private final HighlightState highlight;
        private final BufferedImage hatch = new BufferedImage(RESOLUTION, RESOLUTION, BufferedImage.TYPE_INT_ARGB);
        private final int[] hatchPixels = ((DataBufferInt) hatch.getRaster().getDataBuffer()).getData();
        private double zoom;
        private double x;
        private double y;
        private Point drag;
        private int hoverX = -1;
        private int hoverZ;
        private int highlightedBiome = -1;
        private int highlightedMeta = -1;
        private int highlightedPlacement = -1;
        private boolean generationFinished;

        private MapPanel(BufferedImage image, float[] height, byte[] biomes, byte[] metaBiomes, byte[] placements,
                HighlightState highlight) {
            this.image = image;
            this.height = height;
            this.biomes = biomes;
            this.metaBiomes = metaBiomes;
            this.placements = placements;
            this.highlight = highlight;
            highlight.map = this;
            MouseAdapter mouse = new MouseAdapter() {

                @Override
                public void mousePressed(MouseEvent event) {
                    if (SwingUtilities.isLeftMouseButton(event)) drag = event.getPoint();
                }

                @Override
                public void mouseReleased(MouseEvent event) {
                    drag = null;
                }

                @Override
                public void mouseDragged(MouseEvent event) {
                    if (drag == null) return;
                    x += event.getX() - drag.x;
                    y += event.getY() - drag.y;
                    drag = event.getPoint();
                    repaint();
                }

                @Override
                public void mouseMoved(MouseEvent event) {
                    hoverX = (int) ((event.getX() - x) / zoom);
                    hoverZ = (int) ((event.getY() - y) / zoom);
                    if (hoverX < 0 || hoverX >= RESOLUTION || hoverZ < 0 || hoverZ >= RESOLUTION) hoverX = -1;
                    if (hoverX < 0) highlight.clear();
                    else {
                        int index = hoverZ * RESOLUTION + hoverX;
                        highlight.set(biomes[index] & 255, metaBiomes[index] & 255, placements[index] & 255, true);
                    }
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent event) {
                    hoverX = -1;
                    highlight.clear();
                    repaint();
                }

                @Override
                public void mouseWheelMoved(MouseWheelEvent event) {
                    double factor = Math.pow(1.15, -event.getPreciseWheelRotation());
                    x = event.getX() - (event.getX() - x) * factor;
                    y = event.getY() - (event.getY() - y) * factor;
                    zoom *= factor;
                    repaint();
                }
            };
            addMouseListener(mouse);
            addMouseMotionListener(mouse);
            addMouseWheelListener(mouse);
            addComponentListener(new ComponentAdapter() {

                @Override
                public void componentResized(ComponentEvent event) {
                    zoom = 0;
                    repaint();
                }
            });
        }

        private void setHighlight(int biomeId, int meta, int placement) {
            if (highlightedBiome == biomeId && highlightedMeta == meta && highlightedPlacement == placement) return;
            highlightedBiome = biomeId;
            highlightedMeta = meta;
            highlightedPlacement = placement;
            Arrays.fill(hatchPixels, 0);
            if (biomeId >= 0 || meta >= 0 || placement >= 0) {
                for (int z = 0, i = 0; z < RESOLUTION; z++) {
                    for (int x = 0; x < RESOLUTION; x++, i++) {
                        if ((biomeId < 0 || (biomes[i] & 255) == biomeId) && (meta < 0 || (metaBiomes[i] & 255) == meta)
                                && (placement < 0 || (placements[i] & 255) == placement)
                                && (x + z) % 8 < 2)
                            hatchPixels[i] = 0xA0FFFFFF;
                    }
                }
            }
            repaint();
        }

        private void finishGeneration() {
            generationFinished = true;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (zoom == 0) {
                zoom = Math.min((double) getWidth() / image.getWidth(), (double) getHeight() / image.getHeight());
                x = (getWidth() - image.getWidth() * zoom) / 2;
                y = (getHeight() - image.getHeight() * zoom) / 2;
            }
            Graphics2D map = (Graphics2D) graphics.create();
            map.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            map.translate(x, y);
            map.scale(zoom, zoom);
            map.drawImage(image, 0, 0, null);
            if (generationFinished && (highlightedBiome >= 0 || highlightedMeta >= 0 || highlightedPlacement >= 0)) {
                map.drawImage(hatch, 0, 0, null);
            }
            map.dispose();
            paintTooltip((Graphics2D) graphics);
        }

        private void paintTooltip(Graphics2D graphics) {
            if (hoverX < 0) return;
            int i = hoverZ * RESOLUTION + hoverX;
            int id = biomes[i] & 255;
            int meta = metaBiomes[i] & 255;
            int placement = placements[i] & 255;
            String[] lines = {
                    "X " + (hoverX * BLOCKS_PER_PIXEL - SIZE / 2)
                            + "  Y "
                            + Math.round(height[i])
                            + "  Z "
                            + (hoverZ * BLOCKS_PER_PIXEL - SIZE / 2),
                    "Category: " + placementName(BiomePlacement.values()[placement]) + " " + categoryName(meta),
                    "Biome: " + biomeName(RealisticBiomeBase.getBiome(id)),
                    "Backing biome: " + RealisticBiomeBase.getBiome(id).baseBiome.biomeName };
            FontMetrics metrics = graphics.getFontMetrics();
            int width = 0;
            for (String line : lines) width = Math.max(width, metrics.stringWidth(line));
            int boxX = getWidth() - width - 24, boxY = getHeight() - metrics.getHeight() * lines.length - 18;
            graphics.setColor(new Color(0, 0, 0, 190));
            graphics.fillRoundRect(boxX, boxY, width + 16, metrics.getHeight() * lines.length + 10, 10, 10);
            graphics.setColor(Color.WHITE);
            for (int line = 0; line < lines.length; line++) {
                graphics.drawString(lines[line], boxX + 8, boxY + 5 + metrics.getAscent() + line * metrics.getHeight());
            }
        }
    }
}
