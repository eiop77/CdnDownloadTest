package com.example.cdntest.service;

import jakarta.annotation.PostConstruct;
import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TestService {

  // 파일 경로 url
  String baseUrl = "https://psjktlc92474.edge.naverncp.com/";

  String saveFilePath = "C:\\테스트\\test";

  String speedLogPath = "C:\\테스트\\speed_log.txt";

//  4.38GB acf
  private String acf = "9a693289e4d94e0f81752288df3e7f4d";

  // 810MB acf
//  String acf = "02fa3c60b7984ea08945d60055179cf7";
//  String acc = "2d3007257f78426e95182652486ad4d3";

  // 다운로드 관련 변수

  long elapsedTime = 0;
  long downloadedBytes = 0;
  long prevDownloadedBytes = 0;
  long fileSize;
  Boolean downloadFlag = false;

  Date targetTime;

  @PostConstruct
  public void init() {
    // Calendar 객체 생성 및 한국 시간대로 설정
    Calendar cal = Calendar.getInstance();
    cal.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));

    // 원하는 날짜와 시간 설정
    cal.set(Calendar.YEAR, 2024);
    cal.set(Calendar.MONTH, 6); // 0부터 시작하기 때문에 7월은 6으로 설정
    cal.set(Calendar.DAY_OF_MONTH, 24);
    cal.set(Calendar.HOUR_OF_DAY, 15);
    cal.set(Calendar.MINUTE, 22);
    cal.set(Calendar.SECOND, 32);

    // Calendar 객체로부터 Date 객체 생성
    targetTime = cal.getTime();

    log.info("-------------------------------------------\n");
    log.info("### 현재시간 : " + new Date().toString());
    log.info("### 시작시간 : " + targetTime.toString() + "\n");
    log.info("-------------------------------------------");

    scheduleDownload();
  }

  private void scheduleDownload() {
    // 특정 시간에 다운로드 실행
    Timer timer = new Timer();
    TimerTask task = new TimerTask() {
      @Override
      public void run() {
        try {
          log.info("### 다운로드 시작시간까지 대기 ...");
          downloadFile(acf);
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    };
    timer.schedule(task, targetTime);
  }


  private void downloadFile(String fileId) throws IOException {
    URL url = new URL(baseUrl + fileId);
    URLConnection conn = url.openConnection();

    // 파일 크기 확인
    fileSize = conn.getContentLengthLong();

    try (BufferedInputStream in = new BufferedInputStream(conn.getInputStream());
        FileOutputStream fos = new FileOutputStream(saveFilePath)) {

      byte[] buffer = new byte[1024];
      int readBytes;

      while ((readBytes = in.read(buffer)) != -1) {
        fos.write(buffer, 0, readBytes);
        if (downloadedBytes > 0 && downloadedBytes >= fileSize) {
          downloadFlag = true;
          log.info("\n\n### 다운로드 완료 ###");
          break;
        }
        // 다운로드 진행 상황 업데이트
        downloadedBytes += readBytes;
      }
    }
  }


  @Scheduled(fixedDelay = 1000)
  public void checkDownloadSpeed() throws IOException {

    if (downloadFlag) {
      return;
    }
    if (downloadedBytes == 0) {
      log.info("-------------------------------------------\n");
      log.info("### 현재시간 : " + new Date().toString());
      log.info("### 시작시간 : " + targetTime.toString() + "\n");
      log.info("-------------------------------------------");
      return;
    }

    // 다운로드 속도 측정
    elapsedTime += 1;
    long currentTime = System.currentTimeMillis();
    double speedMBps = (downloadedBytes - prevDownloadedBytes) / (1024.0 * 1024.0);// KB/s 단위로 계산
    double averageSpeedMBps = downloadedBytes / (1024.0 * 1024.0) / elapsedTime;
    String speedStr = String.format("%.2f", speedMBps);
    String averageSpeedStr = String.format("%.2f", averageSpeedMBps);
    log.info("###-------------------------------------###");
    log.info("### 경과시간 : " + elapsedTime + "초         ###");
    log.info("현재 다운로드 속도: " + speedStr + " Mbp/s");
    log.info("평균 다운로드 속도: " + averageSpeedStr + " Mbp/s");
    log.info("###-------------------------------------###");
    log.info("\n");
    // 속도 정보 로그 파일에 저장
    saveSpeedLog(currentTime, downloadedBytes, fileSize, speedStr, averageSpeedStr);

    // 이전 다운로드 진행 상황 및 시간 업데이트
    prevDownloadedBytes = downloadedBytes;

  }


  private void saveSpeedLog(long currentTime, long downloadedBytes, long fileSize, String speedKBps,
      String averageSpeedStr) throws IOException {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    long newFileSize = fileSize / (1024 * 1024);
    long newDownloadedBytes = downloadedBytes / (1024 * 1024);

    String log = "\n--------" + sdf.format(new Date(currentTime)) + "--------- \n "
        + "### 경과시간 : " + elapsedTime + " s\n"
        + " progress : " + newDownloadedBytes + "/" + newFileSize + " Mb\n"
        + " current speed : " + speedKBps + " MB/s\n"
        + " average Speed : " + averageSpeedStr + " MB/s\n";
    try (FileOutputStream fos = new FileOutputStream(speedLogPath, true)) {
      fos.write(log.getBytes());
    }
  }

}
