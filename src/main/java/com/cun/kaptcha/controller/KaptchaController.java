package com.cun.kaptcha.controller;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.cun.kaptcha.utils.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.code.kaptcha.impl.DefaultKaptcha;

@Controller
@RequestMapping("kaptcha")
public class KaptchaController {

    private final Logger logger = LoggerFactory.getLogger(KaptchaController.class);

    /**
     * 验证码工具
     */
    @Autowired
    DefaultKaptcha defaultKaptcha;

    /**
     * redis工具
     */
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    /**
     * 生成验证码
     *
     * @param httpServletRequest  获取ip
     * @param httpServletResponse 传输图片
     * @throws Exception
     */
    @RequestMapping("/img")
    public void img(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
            throws Exception {
        byte[] captchaChallengeAsJpeg = null;
        ByteArrayOutputStream jpegOutputStream = new ByteArrayOutputStream();
        try {
            String createText = defaultKaptcha.createText();
            // 生产验证码字符串并保存到 Redis 中，ip-rightCode，有效期为 1 小时
            String ip = httpServletRequest.getRemoteAddr();
            logger.info("ip：" + ip + "，rightCode = " + createText);
            redisTemplate.opsForValue().set(ip, createText, 1, TimeUnit.HOURS);
            // 使用生产的验证码字符串返回一个BufferedImage对象并转为byte写入到byte数组中
            BufferedImage challenge = defaultKaptcha.createImage(createText);
            ImageIO.write(challenge, "jpg", jpegOutputStream);
        } catch (IllegalArgumentException e) {
            httpServletResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        // 定义response输出类型为image/jpeg类型，使用response输出流输出图片的byte数组
        captchaChallengeAsJpeg = jpegOutputStream.toByteArray();
        httpServletResponse.setHeader("Cache-Control", "no-store");
        httpServletResponse.setHeader("Pragma", "no-cache");
        httpServletResponse.setDateHeader("Expires", 0);
        httpServletResponse.setContentType("image/jpeg");
        ServletOutputStream responseOutputStream = httpServletResponse.getOutputStream();
        responseOutputStream.write(captchaChallengeAsJpeg);
        responseOutputStream.flush();
        responseOutputStream.close();
    }

    /**
     * 校对验证码
     *
     * @param httpServletRequest 获取ip
     * @return
     */
    @ResponseBody
    @RequestMapping("/check/{tryCode}")
    public R check(HttpServletRequest httpServletRequest, @PathVariable String tryCode) {
        String ip = httpServletRequest.getRemoteAddr();
        logger.info("ip：" + ip + "，tryCode = " + tryCode);
        // 从 Redis 中校验
        String rightCode = redisTemplate.opsForValue().get(ip);
        if (rightCode != null && rightCode.equals(tryCode)) {
            return R.ok("校验成功", rightCode);
        }
        return R.error("校验失败");
    }
}