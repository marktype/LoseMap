package net.wyxj.vehicle;

import android.app.AlertDialog;
import android.content.DialogInterface;

// 保存当前Activity的所有控件的类
public class ShowDialog {
    MainActivity context;
    AlertDialog.Builder builder;

    public ShowDialog(MainActivity main) {
        this.context = main;
    }

    /**
     * 弹出一个消息框，消息标题及内容 取决于 传入参数。
     *
     * @param title
     *            标题，为空时为“提醒”
     * @param message
     *            显示在对话框中的提示消息
     */
    public void showMessageBox(String title, String message) {
        if (title == null) {
            title = "提醒";
        }
        builder = new AlertDialog.Builder(this.context);
        builder.setIcon(R.drawable.ic_launcher);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton("确定", null);
        builder.create();
        builder.show();
    }

    /**
     * 显示应用退出 确认框，只有 点击 确定 才 允许 退出
     */
    public void showExitDialog() {
        builder = new AlertDialog.Builder(this.context);
        builder.setIcon(R.drawable.ic_launcher);
        builder.setTitle("提醒");
        builder.setMessage("您确定退出应用？");
        builder.setPositiveButton("确定",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // TODO Auto-generated method stub
                        context.appData.write();
                        context.isCut = false;
                        if (context.mLocationClient.isStarted()) {
                            context.mLocationClient.stop();
                        }
                        context.finish();
                    }
                });
        builder.setNegativeButton("取消", null);
        builder.create();
        builder.show();
    }
}

