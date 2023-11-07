package Nodes.Utils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.Queue;

public class VideoExtractor {
    private final String video;
    private Queue<byte[]> frames;
    private Queue<byte[]> audio;
    private int frameRate;
    private Queue<byte[]> readBytesDirectory(String dir) throws IOException {
        File directory = new File(dir);
        File[] files = directory.listFiles();
        Queue<byte []> list = new LinkedList<>();
        if(files != null)
        {
            for (File file : files) {
                FileInputStream fis = new FileInputStream(file);
                list.add(fis.readAllBytes());
                fis.close();
            }
        }
        return list;
    }
    private void runProcess(String command) throws InterruptedException, IOException {
        ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c", command);
        Process process = processBuilder.start();
        process.waitFor();
        process.destroy();
    }
    private void createDirs(String videoFolder, String audioFolder) throws IOException {
        Files.createDirectory(Path.of(videoFolder));
        Files.createDirectory(Path.of(audioFolder));
    }
    private void extractFrames(String videoFolder) throws IOException, InterruptedException {
        System.out.println("A extrair vídeo");
        String command = "ffmpeg -i " + video + " -vsync vfr -q:v 2 " + videoFolder +"/frame-%04d.png";
        runProcess(command);
        frames = readBytesDirectory(videoFolder);
        System.out.println("Vídeo extraido");
    }
    private void extractAudio(String audioFolder) throws IOException, InterruptedException {
        System.out.println("A extrair audio");
        String command = "ffmpeg -i " + video + " -vn -c:a mp3 -f segment -segment_time 3 " + audioFolder + "/audio%03d.mp3";
        runProcess(command);
        audio = readBytesDirectory(audioFolder);
        System.out.println("Audio extraido");
    }
    private void extractFrameRate() throws IOException, InterruptedException {
        System.out.println("A extrair frame rate");
        String command = "ffprobe -v 0 -of csv=p=0 -select_streams v:0 -show_entries stream=r_frame_rate " + this.video;
        ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c", command);
        Process process = processBuilder.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String[] numbers = reader.readLine().split("/");
        process.waitFor();
        process.destroy();
        reader.close();
        this.frameRate = Integer.parseInt(numbers[0]) / Integer.parseInt(numbers[1]);
        System.out.println("Frame rate: " + this.frameRate);
    }
    private void clean(String videoFolder, String audioFolder) throws IOException, InterruptedException {
        String command1 = "rm -r " + videoFolder;
        String command2 = "rm -r " + audioFolder;
        runProcess(command1);
        runProcess(command2);
    }
    public VideoExtractor(String file) throws IOException, InterruptedException {
        this.video = file;
        String videoFolder = "Content/video" + video;
        String audioFolder = "Content/audio" + video;
        createDirs(videoFolder,audioFolder);
        Thread t1 = new Thread(() -> { try {extractFrames(videoFolder);} catch (IOException | InterruptedException ignored) {}});
        Thread t2 = new Thread(() -> { try {extractAudio(audioFolder);} catch (IOException | InterruptedException ignored) {}});
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        clean(videoFolder,audioFolder);
        extractFrameRate();
        System.out.println("Video " + file + " extraido com sucesso. Número de frames=" + this.frames.size() + ". Número de pedaços de audio com 2 segundos " + this.audio.size());
    }

    public byte[] nextAudio() {
        return this.audio.poll();
    }

    public byte[] nextFrame() {
        return this.frames.poll();
    }

    public boolean hasAudio() {
        return !this.audio.isEmpty();
    }

    public boolean hasFrames() {
        return !this.frames.isEmpty();
    }

    public String getVideo() {
        return video;
    }

    public int getFrameRate() {
        return frameRate;
    }
}
