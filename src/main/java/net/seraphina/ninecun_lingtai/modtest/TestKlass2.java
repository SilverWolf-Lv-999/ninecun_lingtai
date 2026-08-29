package net.seraphina.ninecun_lingtai.modtest;

public class TestKlass2 extends TestKlass1 {
    public static void test1() {
        System.out.println("this is changed");
    }

    public void test2() {
        super.test2();
        System.out.println("114514");
    }
}
