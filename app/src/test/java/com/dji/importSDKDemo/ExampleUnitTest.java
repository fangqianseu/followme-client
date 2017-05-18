package com.dji.importSDKDemo;

import com.amap.api.maps2d.model.LatLng;

import org.junit.Test;


/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() throws Exception {

        LatLng after = CoordinateUtil.toGCJ02Point(30.268000,120.140000);
        System.out.println(after.toString());
    }
}