package lab825;
import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.common.Term;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Simhash {
    // ���ִʵ��ı�
    private String tokens;
    // ʮ���Ƶ�ָ��
    private BigInteger intSimHash;
    // �����Ƶ�ָ��
    private String strSimHash;
    // ������ָ�Ƶ�4����ָ��
    private String strSimHashA;
    private String strSimHashB;
    private String strSimHashC;
    private String strSimHashD;
    private Map<String,Integer> wordCount;
    private int overCount = 5;
    // ָ�Ƶĳ���
    private static int hashbits = 64;
    
	public static void main(String[] args) {
        Simhash SMAlg = new Simhash();
        String string1 = "2018��Ӽ�ʳƷ�չ���ǹ���ɶ�����Ʊͨ��";
        String string2 = "2019��Ӽ�ʳƷ�չ�����ɶ�����Ʊͨ��";
        String finger1 = SMAlg.simHash(string1);
        String finger2 = SMAlg.simHash(string2);
        int LCS=SMAlg.getCommonStrLength(finger1,finger2);
        System.out.println(finger1);
        System.out.println(finger2);
        System.out.println("ԭʼ�ĺ�������Ϊ��"+SMAlg.getDistance(finger1,finger2));
        System.out.println("΢���ĺ�������Ϊ��"+Math.round(1.0*(SMAlg.hashbits-LCS)/SMAlg.hashbits*SMAlg.getDistance(finger1,finger2)));
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

    // ͣ�õĴ���
    private Map<String,String> stopNatures = new HashMap<String, String>();

    // ���Ե�Ȩ��
    private Map<String, Integer> weightOfNature = new HashMap<String, Integer>();


    public void setTokens(String tokens) {
        this.tokens = tokens;
    }

    public void setHashbits(int hashbits) {
        this.hashbits = hashbits;
    }

    private void setMap() {
        // ͣ�ô���Ϊ c:����---> w:���
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
        // ���Ի����ô���Ȩ�أ����ｫn����������Ϊ2����Ĭ��Ȩ��Ϊ1��
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
        // ������������/����
        int[] v = new int[hashbits];
        // ���ı�ȥ����ʽ��, �ִ�.
        List<Term> termList = HanLP.segment(this.tokens);
        for (Term term:termList){
            String word = term.word;
            String nature = term.nature.toString().substring(0, 1);
//             ���˳�Ƶ��
            if (this.wordCount.containsKey(word)) {
                int count = this.wordCount.get(word);
                if (count>this.overCount) {continue;}
                this.wordCount.put(word,count+1);
            }
            else {
                this.wordCount.put(word,1);
            }

            // ����ͣ�ô���
            if (this.stopNatures.containsKey(nature)) {continue;}
            // ��ÿһ���ִ�hashΪһ��̶����ȵ�����.���� 64bit ��һ������.
            BigInteger t = this.hash(word);
            for (int i = 0; i < hashbits; i++) {
                BigInteger bitmask = new BigInteger("1").shiftLeft(i);
                // ����һ������Ϊ64����������(����Ҫ����64λ������ָ��,Ҳ��������������),
                // ��ÿһ���ִ�hash������н����ж�,�����1000...1,��ô����ĵ�һλ��ĩβһλ��1,
                // �м��62λ��һ,Ҳ����˵,��1��1,��0��1.һֱ�������еķִ�hash����ȫ���ж����.
                int weight = 1;
                if (this.weightOfNature.containsKey(nature)) {
                    weight = this.weightOfNature.get(nature);
                }
                if (t.and(bitmask).signum() != 0) {
                    // �����Ǽ��������ĵ�������������������
                    v[i] += weight*term.getFrequency();
                } else {
                    v[i] -= weight*term.getFrequency();
                }
            }
        }
        BigInteger fingerprint = new BigInteger("0");
        StringBuffer simHashBuffer = new StringBuffer();
        for (int i = 0; i < hashbits; i++) {
            // ������������ж�,���ڵ���0�ļ�Ϊ1,С��0�ļ�Ϊ0,�õ�һ�� 64bit ������ָ��/ǩ��.
            if (v[i] >= 0) {
                fingerprint = fingerprint.add(new BigInteger("1").shiftLeft(i));
                simHashBuffer.append("1");
            } else {
                simHashBuffer.append("0");
            }
        }
        this.strSimHash = simHashBuffer.toString();//��һ������ת��Ϊ��Ӧ��64λ�����ƴ�
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
             * ��sourece �ĳ��ȹ��̣��ᵼ��hash�㷨ʧЧ�������Ҫ�Թ��̵Ĵʲ���
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

    // ���ڼ�������Ƶ�hamming����
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
       //����㣺min�Ӵ��ĳ��ȣ�����󳤶ȿ�ʼ
       for (int i = min.length(); i >= 1; i--) {
           //��������Ϊi��min�Ӵ�����0��ʼ
           for (int j = 0; j <= min.length() - i; j++) {  
               target = min.substring(j, j + i);  
               //��������Ϊi��max�Ӵ����ж��Ƿ���target�Ӵ���ͬ����0��ʼ
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
