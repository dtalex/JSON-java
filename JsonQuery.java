package org.json;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JsonQuery {
	private List<String> tokens;
	private Map<String, Object> checkValue = null;
	private final static Pattern pattern = Pattern.compile("\\[(.+?)\\]");
	private Map<String, JsonQuery> subQueries = null;
	
	
	public void setCheckValue(String key, Object value){
		if (checkValue == null){
			checkValue = new HashMap<>();
		}
		checkValue.put(key, value);
	}
	
	
	public JsonQuery(String query) {
		subQueries = new HashMap<>();
		tokens = new ArrayList<>();
			init(query);
		}

	private JsonQuery(String condVar, JsonQuery jsonQuery) {
		tokens = new ArrayList<>();
		this.checkValue = jsonQuery.checkValue;
		jsonQuery.checkValue=this.checkValue;
		init(condVar);
	}


	private Object improvedGet(JSONObject json, String key) {
		if (json == null) return null;
		if (key.matches(".*\\[[0-9]+\\].*")) {
			
			Matcher matcher = pattern.matcher(key);
			matcher.find();
			String id = matcher.group(1);
			JSONArray arr = json.getJSONArray(key.substring(0, key.indexOf("[")));
			return arr.get(Integer.parseInt(id) - 1);
		} else if (key.matches(".*\\[[a-z=_A-Z/\\.0-9:]+\\].*")) {
			Matcher matcher = pattern.matcher(key);
			matcher.find();
			String condition = matcher.group(1);
			String[] conditions = condition.split("=");
			String condVar = conditions[0];
			String condVal = conditions[1];
			if (condVal.startsWith(":")){
				condVal=(String) checkValue.get(condVal);
			}
			JSONArray arr = json.getJSONArray(key.substring(0, key.indexOf("[")));
			JsonQuery query = subQueries.get(condVar);
			for (Object object : arr) {
				JSONObject jObj = (JSONObject) object;
				if (query.evaluatePath((JSONObject) object).equals(condVal)) {
					return jObj;
				}
			}
			return null;
		} else
			return json.opt(key);
	}

	private void init(String path) {
		String[] seq = path.split("\\.(?![^\\[]*\\])");
		for (String string : seq) {
			tokens.add(unescape(string));
			if (string.matches(".*\\[[a-z=_A-Z/\\.0-9:]+\\].*")){
				Matcher matcher = pattern.matcher(string);
				matcher.find();
				String condition = matcher.group(1);
				String[] conditions = condition.split("=");
				String condVar = conditions[0];
				JsonQuery subQuery = new JsonQuery(condVar,this);
				subQueries.put(condVar, subQuery);
				
			}
		}
	}

	private String unescape(String token) {
		return token.replace("~1", "/").replace("~0", "~").replace("\\\"", "\"").replace("\\\\", "\\");
	}

	private String getNextValidPath(Iterator<String> iter) {
		String path = "";
		while ("".equals(path)) {
			path += iter.hasNext() ? iter.next() : "";
		}
		if (path != null && path.matches("^[0-9]+$")) {
			path = "[" + path + "]";
		}
		return path;
	}

	public Object evaluatePath(JSONObject json) {
		return evaluatePath(json, tokens.iterator());
	}

	private Object evaluatePath(JSONObject json, Iterator<String> iter) {
		if (json == null) return null;
		
		String path = getNextValidPath(iter);
		if (!iter.hasNext()) {
			return improvedGet(json, path);
		} else {
			JSONObject nextChild = (JSONObject) improvedGet(json, unescape(path));
			if (nextChild instanceof JSONObject) {
				return evaluatePath(nextChild, iter);
			} else
				return improvedGet(nextChild, unescape(path));
		}
	}
	public static void main(String[] args) {
		JSONObject js = new JSONObject("{     \"db1\":{        \"map1\":{	  \"uri\":\"asdasdad\",	  \"content\":[           {              \"key\":\"key1\",            \"val\":{                 \"no\":\"555222333\",               \"name\":\"jeffrey\"            }         },         {              \"key\":\"key2\",            \"val\":{                 \"no\":\"111777666\",               \"name\":\"kate\"            }         }      ]	  },      \"map2\":[           {              \"key\":\"key1\",            \"val\":\"value1\"         },         {              \"key\":\"key2\",            \"val\":\"value2\"         }      ]   }}");
		
		JsonQuery q = new JsonQuery("db1.map1.content[key=key2].val.name");
		System.out.println(q.evaluatePath(js));
		
		JsonQuery q1 = new JsonQuery("db1.map1.content[val.name=:checkValue].key");
		q1.setCheckValue(":checkValue", "kate");
		System.out.println(q1.evaluatePath(js));
		
		q1.setCheckValue(":checkValue", "jeffrey");
		System.out.println(q1.evaluatePath(js));
		
		JsonQuery q2 = new JsonQuery("db1.map2[2]");
		System.out.println(q2.evaluatePath(js));
		
		return;
	}
}
