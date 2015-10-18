package simpleObjectClient;
public class SerObj implements java.io.Serializable{
    	/**
		 * 
		 */
		private static final long serialVersionUID = 3015081468190405898L;
		private int testInt;
    	private String testString;

    	public SerObj(int i, String s){
    		testInt = i;
    		testString = s;
    	}
    	
    	public String toString(){
    		return testInt + ","+testString;
    	}
    }