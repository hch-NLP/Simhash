package lab825;
import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.common.Term;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Simhash {
    // 待分词的文本
    private String tokens;
    // 十进制的指纹
    private BigInteger intSimHash;
    // 二进制的指纹
    private String strSimHash;
    // 二进制指纹的4个子指纹
    private String strSimHashA;
    private String strSimHashB;
    private String strSimHashC;
    private String strSimHashD;
    private Map<String,Integer> wordCount;
    private int overCount = 5;
    // 指纹的长度
    private static int hashbits = 64;
    
	public static void main(String[] args) {
        Simhash SMAlg = new Simhash();
        String string1 = "2018年加加食品收购金枪鱼获股东大会高票通过";
        String string2 = "2019年加加食品收购案获股东大会高票通过";
        String finger1 = SMAlg.simHash(string1);
        String finger2 = SMAlg.simHash(string2);
        int LCS=SMAlg.getCommonStrLength(finger1,finger2);
        System.out.println(finger1);
        System.out.println(finger2);
        System.out.println("原始的海明距离为："+SMAlg.getDistance(finger1,finger2));
        System.out.println("微调的海明距离为："+Math.round(1.0*(SMAlg.hashbits-LCS)/SMAlg.hashbits*SMAlg.getDistance(finger1,finger2)));
    }

    public BigInteger getIntSimHash(){
        return this.intSimHash;
    }

    public String getStrSimHash() {
        return this.strSimHash;
    }

    private String getStrSimHashA() {
        return this.strSimHashA;
    }

    private String getStrSimHashB() {
        return this.strSimHashB;
    }

    private String getStrSimHashC() {
        return this.strSimHashC;
    }

    private String getStrSimHashD() {
        return this.strSimHashD;
    }

    // 停用的词性
    private Map<String,String> stopNatures = new HashMap<String, String>();

    // 词性的权重
    private Map<String, Integer> weightOfNature = new HashMap<String, Integer>();


    public void setTokens(String tokens) {
        this.tokens = tokens;
    }

    public void setHashbits(int hashbits) {
        this.hashbits = hashbits;
    }

    private void setMap() {
        // 停用词性为 c:连词---> w:标点
    	this.stopNatures.put("a","");
        this.stopNatures.put("c","");
        this.stopNatures.put("e","");
        this.stopNatures.put("h","");
        this.stopNatures.put("k","");
        this.stopNatures.put("m","");
        this.stopNatures.put("o","");
        this.stopNatures.put("p","");
        this.stopNatures.put("q","");
        this.stopNatures.put("r","");
        this.stopNatures.put("u","");
        this.stopNatures.put("w","");
        this.stopNatures.put("x","");
        this.stopNatures.put("y","");
        // 个性化设置词性权重，这里将n：名词设置为2。（默认权重为1）
        this.weightOfNature.put("n",2);
    }

    private String preProcess(String content) {
        String[] strings = {" ","\n","\\r","\\n","\\t","&nbsp","\r","\t"};
        for (String s:strings) {
            content = content.replaceAll(s,"");
        }
        return content.replaceAll("[a-zA-Z]", "");
    }

    public String simHash(String tokens) {
    	this.tokens = preProcess(tokens);
        this.wordCount = new HashMap<String, Integer>();
        setMap();
        // 定义特征向量/数组
        int[] v = new int[hashbits];
        // 将文本去掉格式后, 分词.
        List<Term> termList = HanLP.segment(this.tokens);
        for (Term term:termList){
            String word = term.word;
            String nature = term.nature.toString().substring(0, 1);
//             过滤超频词
            if (this.wordCount.containsKey(word)) {
                int count = this.wordCount.get(word);
                if (count>this.overCount) {continue;}
                this.wordCount.put(word,count+1);
            }
            else {
                this.wordCount.put(word,1);
            }

            // 过滤停用词性
            if (this.stopNatures.containsKey(nature)) {continue;}
            // 将每一个分词hash为一组固定长度的数列.比如 64bit 的一个整数.
            BigInteger t = this.hash(word);
            for (int i = 0; i < hashbits; i++) {
                BigInteger bitmask = new BigInteger("1").shiftLeft(i);
                // 建立一个长度为64的整数数组(假设要生成64位的数字指纹,也可以是其它数字),
                // 对每一个分词hash后的数列进行判断,如果是1000...1,那么数组的第一位和末尾一位加1,
                // 中间的62位减一,也就是说,逢1加1,逢0减1.一直到把所有的分词hash数列全部判断完毕.
                int weight = 1;
                if (this.weightOfNature.containsKey(nature)) {
                    weight = this.weightOfNature.get(nature);
                }
                if (t.and(bitmask).signum() != 0) {
                    // 这里是计算整个文档的所有特征的向量和
                    v[i] += weight*term.getFrequency();
                } else {
                    v[i] -= weight*term.getFrequency();
                }
            }
        }
        BigInteger fingerprint = new BigInteger("0");
        StringBuffer simHashBuffer = new StringBuffer();
        for (int i = 0; i < hashbits; i++) {
            // 最后对数组进行判断,大于等于0的记为1,小于0的记为0,得到一个 64bit 的数字指纹/签名.
            if (v[i] >= 0) {
                fingerprint = fingerprint.add(new BigInteger("1").shiftLeft(i));
                simHashBuffer.append("1");
            } else {
                simHashBuffer.append("0");
            }
        }
        this.strSimHash = simHashBuffer.toString();//把一个整数转化为对应的64位二进制串
        this.strSimHashA = simHashBuffer.substring(0,16);
        this.strSimHashB = simHashBuffer.substring(16,32);
        this.strSimHashC = simHashBuffer.substring(32,48);
        this.strSimHashD = simHashBuffer.substring(48,64);
        return this.getStrSimHashA()+this.getStrSimHashB()+this.getStrSimHashC()+this.getStrSimHashD();
    }



    private BigInteger hash(String source) {

        if (source == null || source.length() == 0) {
            return new BigInteger("0");
        } else {
            /**
             * 当sourece 的长度过短，会导致hash算法失效，因此需要对过短的词补偿
             */
            while (source.length()<3) {
                source = source+source.charAt(0);
            }
            char[] sourceArray = source.toCharArray();
            BigInteger x = BigInteger.valueOf(((long) sourceArray[0]) << 7);
            BigInteger m = new BigInteger("1000003");
            BigInteger mask = new BigInteger("2").pow(this.hashbits).subtract(new BigInteger("1"));
            for (char item : sourceArray) {
                BigInteger temp = BigInteger.valueOf((long) item);
                x = x.multiply(m).xor(temp).and(mask);
            }
            x = x.xor(new BigInteger(String.valueOf(source.length())));
            if (x.equals(new BigInteger("-1"))) {
                x = new BigInteger("-2");
            }
            return x;
        }
    }

    // 用于计算二进制的hamming距离
    public int getDistance(String str1, String str2) {
        int distance;
        if (str1.length() != str2.length()) {
            distance = -1;
        } else {
            distance = 0;
            for (int i = 0; i < str1.length(); i++) {
                if (str1.charAt(i) != str2.charAt(i)) {
                    distance++;
                }
            }
        }
        return distance;
    }
    
	private int getCommonStrLength(String str1, String str2) {
       str1 = str1.toLowerCase();  
       str2 = str2.toLowerCase();  
       int len1 = str1.length();  
       int len2 = str2.length();  
       String min = null;  
       String max = null;  
       String target = null;
       min = len1 <= len2 ? str1 : str2;
       max = len1 >  len2 ? str1 : str2;
       //最外层：min子串的长度，从最大长度开始
       for (int i = min.length(); i >= 1; i--) {
           //遍历长度为i的min子串，从0开始
           for (int j = 0; j <= min.length() - i; j++) {  
               target = min.substring(j, j + i);  
               //遍历长度为i的max子串，判断是否与target子串相同，从0开始
               for (int k = 0; k <= max.length() - i; k++) {  
                   if (max.substring(k,k + i).equals(target)) {  
                       return target.length();  
                   }
               }
           }
       }  
       return 0;  
}
}
