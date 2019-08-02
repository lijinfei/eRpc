package org.ethan.eRpc.comsumerdemo.sao;

import java.util.Map;

import org.ethan.eRpc.common.exception.ERpcException;
import org.ethan.eRpc.consumer.invoke.ERpcConsumerInvoker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 远程服务调用类
 * @author Admin
 *
 */
@Service("testSao")
public class TestSao{
	
	@Autowired
	private ERpcConsumerInvoker invoker;
	
	public Map<String,Object>test1(String p1) throws ERpcException{
		return invoker.invoke("rpcTest1", "1.0", Map.class, p1);
	}
	
}
