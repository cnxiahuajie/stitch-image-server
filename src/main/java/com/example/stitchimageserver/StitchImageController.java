package com.example.stitchimageserver;

import com.alibaba.fastjson.JSONObject;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 图像缝合控制器
 * @author xiahuajie
 */
@Controller
@RequestMapping
public class StitchImageController {

    private static Map<String, JSONObject> imageRepository = new ConcurrentHashMap<>(16);

    /**
     * 生成一个图像数据，包含已处理的原图像地址（随机矩形区域抠除）、y轴坐标（前端显示图像块纵坐标）、图像块宽高值。不包含x轴坐标，避免破解。
     * @return 包含图像数据的json字符串数据
     */
    @RequestMapping("/getImageData")
    @ResponseBody
    public String getImageData() throws IOException {
        JSONObject data = new JSONObject();

        // 编写代码在图片库中随机获取一张图片，然后返回图片的url。注意：此示例中使用的是固定图片，这一段代码只有参考意义，对于功能实现没有实际意义。
        String imageName = "timg";
        String imageSuffix = ".jpeg";
        String imageDirName = String.format("/Users/xiahuajie/Downloads/%s", imageName);
        File imageDir = new File(imageDirName);
        if (imageDir.exists()) {
            imageDir.delete();
        }
        imageDir.mkdir();
        String lock = String.format("lock.jpeg"); // 图片锁（大图）
        String key = String.format("key.jpeg"); // 图片钥匙（小图）

        // 随机生成一个二维坐标，图片块这里使用固定宽100固定高100。
        int x = getRandomNumber(200, 400); // 随机生成横坐标
        int y = getRandomNumber(100, 200); // 随机生成纵坐标
        int width = 100; // 图像块宽度
        int height = 100; // 图像块高度
        String uuid = UUID.randomUUID().toString(); // 图像数据唯一键（使用redis时可以将此作为key）

        ImageUtils.cutImage(imageName + imageSuffix, imageDirName, lock, key, x, y, width, height, imageSuffix.replace(".", ""));

        data.put("x", x);
        data.put("y", y);
        data.put("width", width);
        data.put("height", height);
        data.put("imageName", imageName);
        data.put("uuid", uuid);

        // 存入仓库，可替换为redis
        JSONObject entity = JSONObject.parseObject(data.toJSONString());
        imageRepository.put(uuid, entity);

        // 删除x坐标，避免破解。
        data.remove("x");
        return data.toJSONString();
    }

    /**
     * 获取已处理的图像（大图）
     * @param imageDir 图像目录
     * @param response 图像流
     * @throws IOException
     */
    @RequestMapping("/getLockImage/{imageDir}")
    public void getLockImage(@PathVariable("imageDir") String imageDir, HttpServletResponse response) throws IOException {
        InputStream inputStream = new FileInputStream(String.format("/Users/xiahuajie/Downloads/%s/lock.jpeg", imageDir));
        IOUtils.copy(inputStream, response.getOutputStream());
    }

    /**
     * 获取图像块（小图）
     * @param response 图像块流
     * @throws IOException
     */
    @RequestMapping("/getKeyImage/{imageDir}")
    public void getKeyImage(@PathVariable("imageDir") String imageDir, HttpServletResponse response) throws IOException {
        InputStream inputStream = new FileInputStream(String.format("/Users/xiahuajie/Downloads/%s/key.jpeg", imageDir));
        IOUtils.copy(inputStream, response.getOutputStream());
    }

    /**
     * 校验拼装好的图片
     * @param params 校验参数(uuid表示图像唯一键，x表示用户拖拽的x轴)
     * @return 校验结果(1表示成功，0表示失败)
     */
    @RequestMapping(value = "/check", method = RequestMethod.POST)
    @ResponseBody
    public String check(@RequestBody String params) {
        JSONObject data = new JSONObject();

        JSONObject requestParams = JSONObject.parseObject(params);

        // 从仓库中取出图像信息，并进行校验。
        String uuid = requestParams.getString("uuid");
        Integer x = requestParams.getInteger("x");

        JSONObject imageData = imageRepository.get(uuid);

        // 校验精度
        int accuracy = 3;

        Integer inputX = imageData.getInteger("x");
        if (null == imageData) {
            data.put("checkResult", 0);
        } else if (Math.abs(inputX - x) < accuracy) {
            // x坐标一致校验成功
            data.put("checkResult", 1);
        } else {
            data.put("checkResult", 0);
        }

        return data.toJSONString();
    }

    /**
     * 获取随机数
     * @param min 最小值
     * @param max 最大值
     * @return 随机数
     */
    public int getRandomNumber(int min, int max) {
        return (int) (Math.random()*(max-min)+min);
    }

}
