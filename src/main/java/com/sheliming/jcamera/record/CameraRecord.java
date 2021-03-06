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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.*;

import static org.bytedeco.javacpp.opencv_core.addWeighted;

public class CameraRecord {
    public static final int FRAME_RATE = 50;
    public static final int AUDIO_DEVICE_INDEX = 4;

    public static ExecutorService RecordFrameThreadPool = Executors.newFixedThreadPool(5);
    public static ExecutorService newRecorderThreadPool = Executors.newFixedThreadPool(5);

    public static volatile ConcurrentHashMap<Integer, RecorderObj> recorderHashMap = new ConcurrentHashMap<Integer, RecorderObj>();

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
    public static String recordFrame(Camera camera) {

        RecorderObj recorderObj = new RecorderObj();
        recorderObj.setCamera(camera);

        String saveFileName = camera.getName() + System.currentTimeMillis() + ".mp4";
        recorderObj.setSaveFileName(saveFileName);

        OpenCVFrameGrabber grabber = getOpenCVFrameGrabber(camera.getDeviceId());
        recorderObj.setGrabber(grabber);

        //把摄像头的状态设置为开启
        camera.setState(CamerStatus.open);
        if (recorderHashMap.containsKey(camera.getDeviceId())) {
            return "摄像头正在打开中";
        } else {
            recorderHashMap.put(camera.getDeviceId(), recorderObj);
        }

        RecordFrameThreadPool.submit(new RecordFrameThread(recorderObj));

        //起一个线程打开录制器
        newRecorderThreadPool.submit(new CreateFFmpegRecorderThread(recorderObj));

        return saveFileName;
    }


    public static class RecordFrameThread implements Runnable {
        private RecorderObj recorderObj;

        public RecordFrameThread(RecorderObj recorderObj) {
            this.recorderObj = recorderObj;
        }

        public void run() {
            try {
                RecordFrame cFrame = new RecordFrame(recorderObj);//新建一个窗口
                cFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
                cFrame.setAlwaysOnTop(true);

                long startTime = 0;
                long videoTS = 0;

                OpenCVFrameGrabber grabber = recorderObj.getGrabber();

                // 转换器，用于Frame/Mat/IplImage相互转换
                OpenCVFrameConverter.ToIplImage converter = new OpenCVFrameConverter.ToIplImage();
                // 水印文字位置
                opencv_core.Point point = new opencv_core.Point(450, 30);
                // 颜色，使用黄色
                opencv_core.Scalar scalar = new opencv_core.Scalar(255, 255, 255, 0);
                opencv_core.Mat logo = opencv_imgcodecs.imread("waterLogo.png");
                SimpleDateFormat smft=new SimpleDateFormat("YYYY-MM-dd HH:mm:ss");

                while (true) {
                    if (!cFrame.isDisplayable()) {//窗口是否关闭
                        closeResource(recorderObj,cFrame);
                        System.exit(-1);//退出
                    }

                    Frame frame = grabber.grab();

                    opencv_core.Mat mat = converter.convertToMat(grabber.grabFrame());
                    // 加文字水印，opencv_imgproc.putText（图片，水印文字，文字位置，字体，字体大小，字体颜色，字体粗度，平滑字体，是否翻转文字）
                    opencv_imgproc.putText(mat, smft.format(new Date()), point, opencv_imgproc.CV_FONT_VECTOR0, 0.5, scalar, 1, 20, false);
                    // 定义感兴趣区域(位置，logo图像大小)
                    opencv_core.Mat ROI = mat.apply(new opencv_core.Rect(10, 10, logo.cols(), logo.rows()));
                    addWeighted(ROI, 1, logo, 0.5, 0.0, ROI);

                    System.out.println(recorderObj.getCamera().getState());
                    System.out.println(recorderObj.getRecorder());
                    if (recorderObj.getCamera().getState() == CamerStatus.record) {
                        if (recorderObj.getRecorder() != null) {
                            System.out.println("录制正式开始...");

                            //定义我们的开始时间，当开始时需要先初始化时间戳
                            if (startTime == 0) {
                                startTime = System.currentTimeMillis();
                                addSound(recorderObj.getRecorder());
                            }


                            // 创建一个 timestamp用来写入帧中
                            videoTS = 1000 * (System.currentTimeMillis() - startTime);
                            //检查偏移量
                            if (videoTS > recorderObj.getRecorder().getTimestamp()) {
                                System.out.println("Lip-flap correction: " + videoTS + " : " + recorderObj.getRecorder().getTimestamp() + " -> "
                                        + (videoTS - recorderObj.getRecorder().getTimestamp()));
                                //告诉录制器写入这个timestamp
                                recorderObj.getRecorder().setTimestamp(videoTS);
                            }
                            // 发送帧
                            try {
                                recorderObj.getRecorder().record(frame);
                            } catch (org.bytedeco.javacv.FrameRecorder.Exception e) {
                                System.out.println("录制帧发生异常，什么都不做");
                                e.printStackTrace();
                            }
                        }
                    }

                    cFrame.showImage(frame);//获取摄像头图像并放到窗口上显示， 这里的Frame frame=grabber.grab(); frame是一帧视频图像

                    Thread.sleep(50);//50毫秒刷新一次图像
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void record(RecorderObj recorderObj) {
        System.out.println("开始录制");
        Camera camera = recorderObj.getCamera();
        camera.setState(CamerStatus.record);
    }

    /**
     * 停止录制
     *
     * @param camera
     */
    public static void stopRecord(Camera camera) {
        System.out.println("停止录制");
        if (camera.getState() != CamerStatus.record) {
            System.out.println("摄像头没有打开");
            return;
        }
        camera.setState(CamerStatus.open);
    }

    /**
     * 因为获取FFmpegRecorder时间较长，所以放在单独的线程中去获取
     */
    public static class CreateFFmpegRecorderThread implements Runnable {

        private volatile RecorderObj recorderObj;

        public CreateFFmpegRecorderThread(RecorderObj recorderObj) {
            this.recorderObj = recorderObj;
        }

        public void run() {
            System.out.println("开始初始化recorder");
            OpenCVFrameGrabber grabber = recorderObj.getGrabber();
            int imageWidth = grabber.getImageWidth();
            int imageHeight = grabber.getImageHeight();

            System.out.println("imageWidth:" + imageWidth + "imageHeight:" + imageHeight);

            FFmpegFrameRecorder recorder = getFFmpegFrameRecorder(recorderObj.getSaveFileName(), imageWidth, imageHeight, FRAME_RATE);
            recorderObj.setRecorder(recorder);
            System.out.println("初始化recorder完成:" + recorderObj.getRecorder());

            startRecorder(recorder);
            System.out.println("start recorder 完成");
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

    private static void addSound(final FFmpegFrameRecorder recorder) {
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


    private static FFmpegFrameRecorder getFFmpegFrameRecorder(String outputFile, int captureWidth, int captureHeight, int FRAME_RATE) {
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

    private static void closeResource(RecorderObj recorderObj, CanvasFrame cFrame) {
        FFmpegFrameRecorder recorder = recorderObj.getRecorder();
        OpenCVFrameGrabber grabber = recorderObj.getGrabber();
        cFrame.dispose();
        try {
            if (recorder != null) {
                recorder.stop();
            }
        } catch (FrameRecorder.Exception e) {
            System.out.println("关闭录制器失败");
            try {
                if (recorder != null) {
                    grabber.stop();
                }
            } catch (FrameGrabber.Exception e1) {
                System.out.println("关闭摄像头失败");
                return;
            }
        }
        try {
            if (recorder != null) {
                grabber.stop();
            }
        } catch (FrameGrabber.Exception e) {
            System.out.println("关闭摄像头失败");
        }
    }

    public static void main(String[] args) throws FrameGrabber.Exception, InterruptedException {
//        previewOpencv(0);
//        previewJavacv(0);
        List<Camera> cameraList = CameraFactory.getCameraList();
        System.out.println(recordFrame(cameraList.get(0)));
    }
}
