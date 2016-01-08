package net.wyxj.vehicle;

import android.util.Log;
import android.widget.Toast;

import com.baidu.mapapi.utils.DistanceUtil;
import com.baidu.platform.comapi.basestruct.GeoPoint;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;


/**
 * 道路数据类，用于解析道路数据
 * @author wyxj
 *
 */
public class RoadData {
    public List<List<GeoPoint>> data = null;
    public List<GeoPoint> entry = null;
    public List<GeoPoint> quit = null;
    public List<String> roadName = null;
    public List<Double> radius = null;
    public List<Rect> border= null;

    public InputStream inputStream = null;
    public int currentCurve = -1;
    public int reverse = -1;
    public GeoPoint basePoint;

    public int logi = 0 ;

    private MainActivity mainActivity;

    public RoadData(MainActivity main) {
        mainActivity = main;
        try {
            inputStream = mainActivity.resource.getAssets().open("data.xml");
            Log.v("start info", "find data.xml form assets");
        } catch (IOException e) {
            Toast.makeText(mainActivity, "找不到资源文件，请检查后重新安装程序!",
                    Toast.LENGTH_SHORT).show();
        }
        readData();
        parseData();
    }

    /**
     * 处理data
     */
    private void parseData() {
        // TODO Auto-generated method stub
        radius = new LinkedList<Double>();
        Log.v("test","1");
        for(int i=0; i< data.size() ;i++){
            List<GeoPoint> road = data.get(i);
            int count = 0;
            double minRadius = Double.MAX_VALUE;
            Log.v("test","2");
            for(int j=0;j< road.size()-2;j++){
                double curRadius = getThreePointRadius(road.get(j),road.get(j+1),road.get(j+2) );
                Log.v("current radius", "test"+j+" radius "+curRadius);
                if(curRadius == 0){
                    continue;
                }
                if( curRadius > MainActivity.MaxCurveRadius  || curRadius < MainActivity.MinCurveRadius){
                    continue;
                }
                if( curRadius < minRadius ){
                    minRadius = curRadius ;
                    count++;
                }
            }
            Log.v("test","3");
            Log.v("current radius", roadName.get(i) +" radius "+ minRadius);
            if(count > 0){
                radius.add(minRadius);
            }else{
                radius.add(0.0);
            }
        }
    }

    /**
     * 获得三点的半径，返回0代表无法计算出结果
     */
    public double getThreePointRadius(GeoPoint pt1,GeoPoint pt2,GeoPoint pt3){
        if(currentCurve != -1 ){
            basePoint = entry.get(currentCurve);
        }else{
            basePoint = pt1;
        }
        final double LAT = 0.111132944;
        double x1,x2,x3,y1,y2,y3;
        double baseLat = (double)(basePoint.getLatitudeE6())/1E6;
        double hudu = baseLat*2/Math.PI;
        y1 = LAT*(pt1.getLatitudeE6()-basePoint.getLatitudeE6());
        x1 = LAT*Math.cos(hudu)*(pt1.getLongitudeE6()-basePoint.getLongitudeE6());
        y2 = LAT*(pt2.getLatitudeE6()-basePoint.getLatitudeE6());
        x2 = LAT*Math.cos(hudu)*(pt2.getLongitudeE6()-basePoint.getLongitudeE6());
        y3 = LAT*(pt3.getLatitudeE6()-basePoint.getLatitudeE6());
        x3 = LAT*Math.cos(hudu)*(pt3.getLongitudeE6()-basePoint.getLongitudeE6());
        if(x2==x1||x3==x2||y1==y2||y2==y3){
            return 0;
        }
        double xm1,xm2,ym1,ym2;
        xm1 = (x1+x2)/2;
        xm2 = (x2+x3)/2;
        ym1 = (y1+y2)/2;
        ym2 = (y2+y3)/2;
        double s1,s2,s1c,s2c;
        s1 = (y2-y1)/(x2-x1);
        s2 = (y3-y2)/(x3-x2);
        s1c = -1/s1;
        s2c = -1/s2;
        double x0,y0;
        x0 = ((ym1-ym2)-(s1c*xm1-s2c*xm2))/(s2c-s1c);
        y0 = ym1+s1c*(ym1-ym2+s2c*(xm2-xm1));
        double d1 = Math.sqrt( (x0-x1)*(x0-x1)+(y0-y1)*(y0-y1) );
        double d2 = Math.sqrt( (x0-x2)*(x0-x2)+(y0-y2)*(y0-y2) );
        double d3 = Math.sqrt( (x0-x3)*(x0-x3)+(y0-y3)*(y0-y3) );
        return (d1+d2+d3)/3;
    }

    /**
     * 获取前方弯道半径
     * @param pt 目前所处的坐标点
     * @return 前方弯道半径,返回0代表无法计算
     */
    public double getRadius(GeoPoint pt){
        logi++;
        // 首先判断 pt是否处在目前标定的弯道的矩阵内，如果不在，那么判定已经超出弯道，需要重新计算所处的弯道
        if(currentCurve >= 0  && border.get(currentCurve).contain(pt) ){
            if( logi % 10 == 3 )
                Log.v("radius","curve right "+ radius.get(currentCurve));
            return radius.get(currentCurve);
        }
        // 如果能判断目前位于一个唯一的矩阵中
        int count = 0;
        int cur = -1;
        List<Integer> q = new LinkedList<Integer>();
        for( int i=0;i<border.size();i++){
            if( border.get(i).contain(pt)){
                if( logi % 15 == 7 )
                    Log.v("radius","you in rect " + roadName.get(i) );
                count ++;
                cur = i;
                q.add(count);
            }
        }
        // 如果是 1 就是唯一的矩阵，判定成功
        if( count==1){
            if( logi % 10 == 3 )
                Log.v("radius","one in rect " + radius.get(cur));
            return radius.get(cur);
        }else if(count == -1){
            if( logi % 10 == 3 )
                Log.v("radius","not in curve - 0");
            return 0;
        }
        // 下面开始搜索最近的点
        count = 0;
        cur = -1;
        double minDist = Double.MAX_VALUE;
        for( int i=0;i<q.size();i++){
            int curRoadId = q.get(i);
            List<GeoPoint> road = data.get(curRoadId);
            for(int j=0;j<road.size();j++){
                GeoPoint pt1 = road.get(j);
                double curDist = DistanceUtil.getDistance(pt, pt1);
                if( curDist < minDist){
                    minDist = curDist;
                    cur = curRoadId;
                }
            }
        }
        if( cur != -1){
            if( logi % 10 == 3 )
                Log.v("radius","min dist "+radius.get(cur));
            return radius.get(cur);
        }
        if( logi % 10 == 3 )
            Log.v("radius","you got 0 ,i think is wrong");
        return 0;
    }

    public double getDistance(GeoPoint pt1,GeoPoint pt2){
        final double LAT = 105500;
        double la1 = (double)(pt1.getLatitudeE6())/1E6;
        double lo1 = (double)(pt1.getLongitudeE6())/1E6;
        double la2 = (double)(pt2.getLatitudeE6())/1E6;
        double lo2 = (double)(pt2.getLongitudeE6())/1E6;
        double aveLa = (la1 + la2)/2;
        double a1 = LAT*Math.abs(la2-la1) * LAT*Math.abs(la2-la1);
        double a2 = LAT*Math.abs(lo2-lo1) * LAT*Math.abs(lo2-lo1) *
                Math.cos(aveLa*2/Math.PI) * Math.cos(aveLa*2/Math.PI);
        return Math.sqrt( a1+a2 );
    }

    /**
     * 该方法设置的必须是当前所在的点。
     * @param curPoint
     */
    public void setCurrentPoint(GeoPoint curPoint){
        // 临时变量，存储目前为止距离最短的弯道id
        int tempMinCurveId = -1;
        double tempMinCurveDist = MainActivity.MinCurveEntryDist;
        // 下面进行弯道判断，用于监测目前处于哪条弯道上,暂时规定50m 内才有效
        for(int i = 0; i < mainActivity.roadData.entry.size() ; i++ ){
            double tempDist = DistanceUtil.getDistance( entry.get(i), curPoint);
            if(mainActivity.appData.lastLocID %20 == 1){
                Log.v("location distance info", ""+(int)tempDist+"m about " + entry.get(i).toString() );
            }
            // 如果计算出来的距离 比 50米或者是之前更小的结果海小，就取这个更小的结果
            if( tempDist < tempMinCurveDist ){
                tempMinCurveId = i;
                tempMinCurveDist = tempDist;
            }
        }
        // 如果id ！= -1 说明，被判断已经进入了弯道,并且如果前面进入了某条弯道，那么弯道上一次的id跟现在不能一样
        if(  tempMinCurveId != -1 && tempMinCurveId != mainActivity.roadData.currentCurve){
            currentCurve = tempMinCurveId ;
            reverse = 0;	// 表示当前顺行
            // 输出调试信息，
            Log.v("entry new curve","current id:" + tempMinCurveId +" ,name:"+ roadName.get(tempMinCurveId) );
        }else{
            // 如果没办法判断入口点，那么尝试搜索出口点，
            for(int i = 0; i < mainActivity.roadData.quit.size() ; i++ ){
                double tempDist = DistanceUtil.getDistance( entry.get(i), curPoint);
                if(mainActivity.appData.lastLocID %20 == 15){
//					Log.v("reverse location distance info", ""+(int)tempDist+"m about " + entry.get(i).toString() );
                }
                if( tempDist < tempMinCurveDist ){
                    tempMinCurveId = i;
                    tempMinCurveDist = tempDist;
                }
            }
            // 注意如果出口点标注的id是入口点的id，那么显然此时不是进入弯道
            if( tempMinCurveId != -1 && tempMinCurveId != mainActivity.roadData.currentCurve ){
                currentCurve = tempMinCurveId ;
                reverse = 1;	// 表示当前逆行
                // 输出调试信息，
//				Log.v("entry new curve but reverse","current id:" + tempMinCurveId +" ,name:"+ roadName.get(tempMinCurveId) );
            }
        }

    }

    private void readData() {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = null;
        Document document = null;
        try {
            builder = factory.newDocumentBuilder();
            document = builder.parse(inputStream);
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }

        Log.v("start info", "start to parse xml file");
        // root 代表data元素
        Element root = document.getDocumentElement();
        // rootNodeList 代表data中的空文本以及 road元素
        NodeList rootNodeList = root.getChildNodes();

        data = new LinkedList<List<GeoPoint>>();
        roadName = new LinkedList<String>();
        entry = new LinkedList<GeoPoint>();
        quit = new LinkedList<GeoPoint>();
        // 存储 各条弯道的区间范围
        border = new LinkedList<Rect>();

        for(int i = 0; i < rootNodeList.getLength() ; i++){
            Node roadNode = rootNodeList.item(i);
            if( roadNode.getNodeType() != Node.ELEMENT_NODE){
                continue;
            }
            // 代表这条路的名字
            String roadNameText = ((Element)roadNode).getAttribute("name");
            NodeList roadNodeList = roadNode.getChildNodes();

            List<GeoPoint> road = new LinkedList<GeoPoint>();
            roadName.add(roadNameText);
            Rect rect = new Rect();

            for(int j = 0; j < roadNodeList.getLength() ; j++){
                Node pointNode = roadNodeList.item(j);
                if( pointNode.getNodeType() != Node.ELEMENT_NODE){
                    continue;
                }
                Element pointElement = (Element)pointNode;
                // 代表这条经纬度的坐标类型，分entry,curve,quit
                String pointType = pointElement.getAttribute("type");
                // 代表这条路的经纬度内容
                String pointText = ((Text)pointElement.getFirstChild()).getWholeText();
                String[] tempStrings = 	pointText.split(",");
                // 经度
                int longitude = (int)(Double.parseDouble(tempStrings[0])*1E6);
                /** 纬度  */
                int latitude  = (int)(Double.parseDouble(tempStrings[1])*1E6);
                GeoPoint tempPoint = new GeoPoint(latitude,longitude);
                if( pointType.equals("entry") ){
                    entry.add( tempPoint );
                }else if( pointType.equals("quit") ){
                    quit.add( tempPoint );
                }
                int dist = 100;
                // longitude以及latitude即每个点的经纬度
                if( longitude < rect.left){
                    rect.left = longitude-dist;
                }
                if( longitude > rect.right){
                    rect.right = longitude+dist;
                }
                if( latitude > rect.top){
                    rect.top = latitude+dist;
                }
                if( latitude < rect.bottom){
                    rect.bottom = latitude-dist;
                }
                road.add(tempPoint);
            }
            Log.v("parse info", roadNameText+" contain "+road.size()+" point" );
            data.add(road);
            border.add(rect);
            Log.v("rect info" ,roadNameText + "("+rect.left+","+rect.top+"),("+rect.right+","+rect.bottom+")" );
        }
        Log.v("parse info", "data total contain "+data.size()+" road data");
    }
}

class Rect{
    public int left,right,top,bottom;
    public Rect(int a,int b,int c,int d){
        left = a;
        top = b;
        right = c;
        bottom = d;
    }
    public Rect() {
        // TODO Auto-generated constructor stub
        left = Integer.MAX_VALUE;
        top = Integer.MIN_VALUE;
        right =  Integer.MIN_VALUE;
        bottom = Integer.MAX_VALUE;
    }
    /**
     * 判断GeoPoint是否处在这个矩形区域内
     * @param point
     * @return false代表不在此内，true代表在矩阵内
     */
    public boolean contain(GeoPoint point){
        int longitude = point.getLongitudeE6();
        int latitude = point.getLatitudeE6();
        if( longitude < left || longitude > right){
            return false;
        }
        if( latitude > top || latitude < bottom){
            return false;
        }
        return true;
    }
}

