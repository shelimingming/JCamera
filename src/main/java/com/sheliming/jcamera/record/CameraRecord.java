package com.sheliming.jcamera.record;

import com.sheliming.jcamera.CamerStatus;
import com.sheliming.jcamera.Camera;
import com.sheliming.jcamera.CameraFactory;
import com.sheliming.jcamera.swing.RecordFrame;
import org.bytedeco.javacpp.*;
import org.bytedeco.javacv.*;

import javax.sound.sampled.*;
import javax.swing.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.*;

public class CameraRecord {
    public static final int FRAME_RATE = 50;

    public static ExecutorService RecordThreadPool = Executors.newFixedThreadPool(5);
    public static ExecutorService RecordFrameThreadPool = Executors.newFixedThreadPool(5);

    public static HashMap<Integer, LinkedBlockingQueue<Frame>> hashmap = new HashMap<Integer, LinkedBlockingQueue<Frame>>();

    /**
     * 预览摄像头
     */
    public static void previewOpencv(Camera camera) {
        long startTime = System.currentTimeMillis();

        opencv_videoio.VideoCapture vc = new opencv_videoio.VideoCapture(camera.getDeviceId());
        CanvasFrame cFrame = new CanvasFrame("opencv自带", CanvasFrame.getDefaultGamma() / 2.2);

        //javacv提供的转换器，方便mat转换为Frame
        OpenCVFrameConverter.ToIplImage converter = new OpenCVFrameConverter.ToIplImage();
        opencv_core.Mat mat = new opencv_core.Mat();


        double fpsStart = System.currentTimeMillis();
        double fpsEnd;
        while (true) {
            if (!cFrame.isVisible()) {//窗口是否关闭
                vc.release();//停止抓取
                return;
            }
            vc.retrieve(mat);//重新获取mat
            if (vc.grab()) {//是否采集到摄像头数据
                if (vc.read(mat)) {//读取一帧mat图像
                    cFrame.showImage(converter.convert(mat));
                }
                mat.release();//释放mat
            }
            fpsEnd = System.currentTimeMillis();
            System.out.println("fps:" + 1000.0 / (fpsEnd - fpsStart));
            fpsStart = fpsEnd;

            long endTime = System.currentTimeMillis();
            System.out.println("摄像头启动时间： " + (endTime - startTime) + "ms");

            //每45毫秒捕获一帧
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }

    }

    /**
     * 预览摄像头
     */
    public static void previewJavacv(Camera camera) throws FrameGrabber.Exception, InterruptedException {
        long startTime = System.currentTimeMillis();

        OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(camera.getDeviceId());
        grabber.start();   //开始获取摄像头数据
        CanvasFrame canvas = new CanvasFrame("摄像头");//新建一个窗口
        canvas.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        canvas.setAlwaysOnTop(true);

        double fpsStart = System.currentTimeMillis();
        double fpsEnd;
        while (true) {
            if (!canvas.isDisplayable()) {//窗口是否关闭
                grabber.stop();//停止抓取
                System.exit(-1);//退出
            }

            Frame frame = grabber.grab();

            canvas.showImage(frame);//获取摄像头图像并放到窗口上显示， 这里的Frame frame=grabber.grab(); frame是一帧视频图像

            fpsEnd = System.currentTimeMillis();
            System.out.println("fps:" + 1000.0 / (fpsEnd - fpsStart));
            fpsStart = fpsEnd;

            long endTime = System.currentTimeMillis();
            System.out.println("摄像头启动时间： " + (endTime - startTime) + "ms");
            Thread.sleep(50);//50毫秒刷新一次图像
        }

    }

    /**
     * 打开录制界面
     *
     * @param camera
     * @return
     * @throws FrameGrabber.Exception
     * @throws InterruptedException
     */
    public static String recordFrame(Camera camera) throws FrameGrabber.Exception, InterruptedException {
        OpenCVFrameGrabber grabber = getOpenCVFrameGrabber(camera.getDeviceId());

        //把摄像头的状态设置为开启
        camera.setState(CamerStatus.open);
        if (hashmap.containsKey(camera.getDeviceId())) {
            return "摄像头正在打开中";
        } else {
            hashmap.put(camera.getDeviceId(), new LinkedBlockingQueue<Frame>());
        }

        RecordFrameThreadPool.submit(new RecordFrameThread(camera, grabber));
        return "开启成功";
    }


    public static String record(Camera camera, OpenCVFrameGrabber grabber) {


        RecordThreadPool.submit(new RecordThread(camera, FRAME_RATE, grabber));
        System.out.println(" ===> main Thread execute here ! ");


        return null;
    }

    public static class RecordFrameThread implements Runnable {

        private Camera camera;
        private OpenCVFrameGrabber grabber;

        public RecordFrameThread(Camera camera, OpenCVFrameGrabber grabber) {
            this.camera = camera;
            this.grabber = grabber;
        }

        public void run() {
            try {
                LinkedBlockingQueue<Frame> linkedBlockingQueue = hashmap.get(camera.getDeviceId());

                RecordFrame cFrame = new RecordFrame(camera, grabber);//新建一个窗口
                cFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
                cFrame.setAlwaysOnTop(true);


                while (true) {
                    if (!cFrame.isDisplayable()) {//窗口是否关闭
                        grabber.stop();//停止抓取
                        System.exit(-1);//退出
                    }

                    Frame frame = grabber.grab();

                    if (camera.getState() == CamerStatus.record) {
                        linkedBlockingQueue.put(frame);
                    }

                    cFrame.showImage(frame);//获取摄像头图像并放到窗口上显示， 这里的Frame frame=grabber.grab(); frame是一帧视频图像

                    Thread.sleep(50);//50毫秒刷新一次图像
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static class RecordThread implements Runnable {
        private static final int AUDIO_DEVICE_INDEX = 4;
        private Camera camera;
        private int frameRate;
        private OpenCVFrameGrabber grabber;

        public RecordThread(Camera camera, int frameRate, OpenCVFrameGrabber grabber) {
            this.camera = camera;
            this.frameRate = frameRate;
            this.grabber = grabber;
        }

        public void run() {
            try {
                LinkedBlockingQueue<Frame> linkedBlockingQueue = hashmap.get(camera.getDeviceId());
                Loader.load(opencv_objdetect.class);

                OpenCVFrameConverter.ToIplImage converter = new OpenCVFrameConverter.ToIplImage();//转换器
                opencv_core.IplImage grabbedImage = converter.convert(grabber.grab());//抓取一帧视频并将其转换为图像，至于用这个图像用来做什么？加水印，人脸识别等等自行添加
                int width = grabbedImage.width();
                int height = grabbedImage.height();

                final FFmpegFrameRecorder recorder = getfFmpegFrameRecorder("11111.mp4", width, height, FRAME_RATE);


                if (startRecorder(recorder)) return;

                addSound(AUDIO_DEVICE_INDEX, FRAME_RATE, recorder);

                long startTime = 0;
                long videoTS = 0;
                org.bytedeco.javacv.Frame rotatedFrame = converter.convert(grabbedImage);//不知道为什么这里不做转换就不能推到rtmp
                int i = 0;

                //把摄像头的状态设为录像
                camera.setState(CamerStatus.record);

                while (camera.getState() == CamerStatus.record && (grabbedImage = converter.convert(linkedBlockingQueue.take())) != null) {
                    i++;
                    System.out.println(i);
                    rotatedFrame = converter.convert(grabbedImage);
                    if (startTime == 0) {
                        startTime = System.currentTimeMillis();
                    }
                    recorder.setTimestamp(videoTS);
                    videoTS = 1000 * (System.currentTimeMillis() - startTime);
                    recorder.record(rotatedFrame);
                    //Thread.sleep(40);
                }

                System.out.println("录制结束");
                recorder.stop();
                recorder.release();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (FrameRecorder.Exception e) {
                e.printStackTrace();
            } catch (FrameGrabber.Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 停止录制
     *
     * @param camera
     */
    public static void stopRecord(Camera camera) {
        if (camera.getState() != CamerStatus.record) {
            System.out.println("摄像头没有打开");
            return;
        }
        camera.setState(CamerStatus.open);

        LinkedBlockingQueue<Frame> linkedBlockingQueue = hashmap.get(camera.getDeviceId());
        try {
            linkedBlockingQueue.put(new Frame());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    private static OpenCVFrameGrabber getOpenCVFrameGrabber(int deviceId) {
        /**
         * FrameGrabber 类包含：OpenCVFrameGrabber
         * (opencv_videoio),C1394FrameGrabber, FlyCaptureFrameGrabber,
         * OpenKinectFrameGrabber,PS3EyeFrameGrabber,VideoInputFrameGrabber, 和
         * FFmpegFrameGrabber.
         */
        OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(deviceId);
        System.out.println("开始抓取摄像头...");
        int isTrue = 0;// 摄像头开启状态
        try {
            grabber.start();
            isTrue += 1;
        } catch (FrameGrabber.Exception e2) {
            if (grabber != null) {
                try {
                    grabber.restart();
                    isTrue += 1;
                } catch (FrameGrabber.Exception e) {
                    isTrue -= 1;
                    try {
                        grabber.stop();
                    } catch (FrameGrabber.Exception e1) {
                        isTrue -= 1;
                    }
                }
            }
        }
        if (isTrue < 0) {
            System.err.println("摄像头首次开启失败，尝试重启也失败！");
            return null;
        } else if (isTrue < 1) {
            System.err.println("摄像头开启失败！");
            return null;
        } else if (isTrue == 1) {
            System.err.println("摄像头开启成功！");
        } else if (isTrue == 1) {
            System.err.println("摄像头首次开启失败，重新启动成功！");
        }
        return grabber;
    }

    private static void addSound(final int AUDIO_DEVICE_INDEX, final int FRAME_RATE, final FFmpegFrameRecorder recorder) {
        // 音频捕获
        new Thread(new Runnable() {
            public void run() {
                /**
                 * 设置音频编码器 最好是系统支持的格式，否则getLine() 会发生错误
                 * 采样率:44.1k;采样率位数:16位;立体声(stereo);是否签名;true:
                 * big-endian字节顺序,false:little-endian字节顺序(详见:ByteOrder类)
                 */
                AudioFormat audioFormat = new AudioFormat(44100.0F, 16, 2, true, false);

                // 通过AudioSystem获取本地音频混合器信息
                Mixer.Info[] minfoSet = AudioSystem.getMixerInfo();
                // 通过AudioSystem获取本地音频混合器
                Mixer mixer = AudioSystem.getMixer(minfoSet[AUDIO_DEVICE_INDEX]);
                // 通过设置好的音频编解码器获取数据线信息
                DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, audioFormat);
                try {
                    // 打开并开始捕获音频
                    // 通过line可以获得更多控制权
                    // 获取设备：TargetDataLine line
                    // =(TargetDataLine)mixer.getLine(dataLineInfo);
                    final TargetDataLine line = (TargetDataLine) AudioSystem.getLine(dataLineInfo);
                    line.open(audioFormat);
                    line.start();
                    // 获得当前音频采样率
                    final int sampleRate = (int) audioFormat.getSampleRate();
                    // 获取当前音频通道数量
                    final int numChannels = audioFormat.getChannels();
                    // 初始化音频缓冲区(size是音频采样率*通道数)
                    int audioBufferSize = sampleRate * numChannels;
                    final byte[] audioBytes = new byte[audioBufferSize];

                    ScheduledThreadPoolExecutor exec = new ScheduledThreadPoolExecutor(1);
                    exec.scheduleAtFixedRate(new Runnable() {
                        public void run() {
                            try {
                                // 非阻塞方式读取
                                int nBytesRead = line.read(audioBytes, 0, line.available());
                                // 因为我们设置的是16位音频格式,所以需要将byte[]转成short[]
                                int nSamplesRead = nBytesRead / 2;
                                short[] samples = new short[nSamplesRead];
                                /**
                                 * ByteBuffer.wrap(audioBytes)-将byte[]数组包装到缓冲区
                                 * ByteBuffer.order(ByteOrder)-按little-endian修改字节顺序，解码器定义的
                                 * ByteBuffer.asShortBuffer()-创建一个新的short[]缓冲区
                                 * ShortBuffer.get(samples)-将缓冲区里short数据传输到short[]
                                 */
                                ByteBuffer.wrap(audioBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(samples);
                                // 将short[]包装到ShortBuffer
                                ShortBuffer sBuff = ShortBuffer.wrap(samples, 0, nSamplesRead);
                                // 按通道录制shortBuffer
                                recorder.recordSamples(sampleRate, numChannels, sBuff);
                            } catch (FrameRecorder.Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }, 0, (long) 1000 / FRAME_RATE, TimeUnit.MILLISECONDS);
                } catch (LineUnavailableException e1) {
                    e1.printStackTrace();
                }
            }
        }).start();
    }

    private static FFmpegFrameRecorder getfFmpegFrameRecorder(String outputFile, int captureWidth, int captureHeight, int FRAME_RATE) {
        /**
         * FFmpegFrameRecorder(String filename, int imageWidth, int imageHeight,
         * int audioChannels) fileName可以是本地文件（会自动创建），也可以是RTMP路径（发布到流媒体服务器）
         * imageWidth = width （为捕获器设置宽） imageHeight = height （为捕获器设置高）
         */
        final FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(outputFile, captureWidth, captureHeight, 2);
        recorder.setInterleaved(true);

        /**
         * 该参数用于降低延迟 参考FFMPEG官方文档：https://trac.ffmpeg.org/wiki/StreamingGuide
         * 官方原文参考：ffmpeg -f dshow -i video="Virtual-Camera" -vcodec libx264
         * -tune zerolatency -b 900k -f mpegts udp://10.1.0.102:1234
         */

        recorder.setVideoOption("tune", "zerolatency");
        /**
         * 权衡quality(视频质量)和encode speed(编码速度) values(值)：
         * ultrafast(终极快),superfast(超级快), veryfast(非常快), faster(很快), fast(快),
         * medium(中等), slow(慢), slower(很慢), veryslow(非常慢)
         * ultrafast(终极快)提供最少的压缩（低编码器CPU）和最大的视频流大小；而veryslow(非常慢)提供最佳的压缩（高编码器CPU）的同时降低视频流的大小
         * 参考：https://trac.ffmpeg.org/wiki/Encode/H.264 官方原文参考：-preset ultrafast
         * as the name implies provides for the fastest possible encoding. If
         * some tradeoff between quality and encode speed, go for the speed.
         * This might be needed if you are going to be transcoding multiple
         * streams on one machine.
         */
        recorder.setVideoOption("preset", "ultrafast");
        /**
         * 参考转流命令: ffmpeg
         * -i'udp://localhost:5000?fifo_size=1000000&overrun_nonfatal=1' -crf 30
         * -preset ultrafast -acodec aac -strict experimental -ar 44100 -ac
         * 2-b:a 96k -vcodec libx264 -r 25 -b:v 500k -f flv 'rtmp://<wowza
         * serverIP>/live/cam0' -crf 30
         * -设置内容速率因子,这是一个x264的动态比特率参数，它能够在复杂场景下(使用不同比特率，即可变比特率)保持视频质量；
         * 可以设置更低的质量(quality)和比特率(bit rate),参考Encode/H.264 -preset ultrafast
         * -参考上面preset参数，与视频压缩率(视频大小)和速度有关,需要根据情况平衡两大点：压缩率(视频大小)，编/解码速度 -acodec
         * aac -设置音频编/解码器 (内部AAC编码) -strict experimental
         * -允许使用一些实验的编解码器(比如上面的内部AAC属于实验编解码器) -ar 44100 设置音频采样率(audio sample
         * rate) -ac 2 指定双通道音频(即立体声) -b:a 96k 设置音频比特率(bit rate) -vcodec libx264
         * 设置视频编解码器(codec) -r 25 -设置帧率(frame rate) -b:v 500k -设置视频比特率(bit
         * rate),比特率越高视频越清晰,视频体积也会变大,需要根据实际选择合理范围 -f flv
         * -提供输出流封装格式(rtmp协议只支持flv封装格式) 'rtmp://<FMS server
         * IP>/live/cam0'-流媒体服务器地址
         */
        recorder.setVideoOption("crf", "25");
        // 2000 kb/s, 720P视频的合理比特率范围
        recorder.setVideoBitrate(2000000);
        // h264编/解码器
        recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
        // 封装格式flv
        recorder.setFormat("flv");
        // 视频帧率(保证视频质量的情况下最低25，低于25会出现闪屏)
        recorder.setFrameRate(FRAME_RATE);
        // 关键帧间隔，一般与帧率相同或者是视频帧率的两倍
        recorder.setGopSize(FRAME_RATE * 2);
        // 不可变(固定)音频比特率
        recorder.setAudioOption("crf", "0");
        // 最高质量
        recorder.setAudioQuality(0);
        // 音频比特率
        recorder.setAudioBitrate(192000);
        // 音频采样率
        recorder.setSampleRate(44100);
        // 双通道(立体声)
        recorder.setAudioChannels(2);
        // 音频编/解码器
        recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);
        System.out.println("开始录制...");
        return recorder;
    }


    private static boolean startRecorder(FFmpegFrameRecorder recorder) {
        try {
            recorder.start();
        } catch (FrameRecorder.Exception e2) {
            if (recorder != null) {
                System.out.println("关闭失败，尝试重启");
                try {
                    recorder.stop();
                    recorder.start();
                } catch (FrameRecorder.Exception e) {
                    try {
                        System.out.println("开启失败，关闭录制");
                        recorder.stop();
                        return true;
                    } catch (FrameRecorder.Exception e1) {
                        return true;
                    }
                }
            }

        }
        return false;
    }

    public static void main(String[] args) throws FrameGrabber.Exception, InterruptedException {
//        previewOpencv(0);
//        previewJavacv(0);
        List<Camera> cameraList = CameraFactory.getCameraList();
        System.out.println(recordFrame(cameraList.get(0)));
    }
}
