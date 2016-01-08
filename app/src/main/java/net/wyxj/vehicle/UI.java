package net.wyxj.vehicle;

/**
 * @author
 */
public class UI {
    MainActivity context;
    public int currentMode;
    static final int MAIN = 0;
    static final int MENU = 1;

    public UI(MainActivity con) {
        context = con;
        initMain();
    }

    /**
     * 对初始化界面进行合理设置
     */
    public void initMain() {
        // 设置 layout 以及更改当前界面模式 为 main 模式
        context.setContentView(R.layout.activity_main);
        currentMode = MAIN;
    }
}
