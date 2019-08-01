package org.ethan.eRpc.consumer.invoke;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.ethan.eRpc.consumer.cache.ProviderCache;
import org.ethan.eRpc.consumer.cache.ProviderCacheFactory;
import org.ethan.eRpc.consumer.socket.Client;
import org.ethan.eRpc.core.ERpcException;
import org.ethan.eRpc.core.bean.ServiceBean;
import org.ethan.eRpc.core.bean.ServiceBean.Host;
import org.ethan.eRpc.core.exporter.LocalExporter;
import org.ethan.eRpc.core.request.ERpcRequest;
import org.ethan.eRpc.core.response.ERpcResponse;
import org.ethan.eRpc.core.serialize.ERpcSerialize;
import org.ethan.eRpc.core.serialize.ERpcSerializeException;
import org.ethan.eRpc.core.util.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class ERpcInvoker implements InitializingBean,ApplicationContextAware{
	
	private static final Logger logger = LoggerFactory.getLogger(ERpcInvoker.class);
			
	@Autowired
	private LocalExporter localExporter;
	
	private ApplicationContext applicationContext;
	
	private ProviderCache providerCache;
	
	private ERpcSerialize serializer;
	
	
	
	
	public<T> T invoke(String serviceName,String version,Class<T> classes,Object... params) throws ERpcException {
		//先尝试本地invoke
		ServiceBean serviceBean = localExporter.getServiceBean(serviceName, version);
		if(serviceBean != null) {
			logger.info("Find service["+serviceName+"]with version["+version+"] in local,try local invoke");
			return localInvoke(serviceBean,classes,params);
		}else {
			//未找到本地服务，尝试远程调用
			Host host = providerCache.getProvider(serviceName, version);
			if(host == null) {
				throw new ERpcException("No provider found for service["+serviceName+"] and version["+version+"]");
			}
			try {
				return invoke(host, serviceName,classes,version, params);
			} catch (InterruptedException | IOException | ERpcSerializeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}
	}
	
	private<T> T localInvoke(ServiceBean serviceBean,Class<T> classes,Object... params) throws ERpcException {
		Object controller = null;
		try {
			controller = applicationContext.getBean(serviceBean.getBeanName());
		} catch (BeansException e1) {
			// TODO Auto-generated catch block
			logger.error("Error occurs when getBean",e1);
		}
		
		if(controller == null) {
			throw new ERpcException("Controller bean["+serviceBean.getBeanName()+"] not found!");
		}
		
		Method serviceMethod = serviceBean.getServiceMethod();
		
		try {			
			Object result = serviceMethod.invoke(controller, params);
			
			return serializer.respBodyDeSerialize(serializer.respBodySerialize(result), classes);
			
		} catch (ERpcSerializeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new ERpcException("Error occurs when  Serialize !",e);
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	private<T> T invoke(Host host,String serviceName,Class<T> classes,String version,Object... params) throws UnsupportedEncodingException, InterruptedException, IOException, ERpcSerializeException, ERpcException {
		ERpcRequest request = new ERpcRequest();
		ERpcRequest.Header header = new ERpcRequest.Header();
		String eRpcId = UUID.randomUUID().toString();
		header.seteRpcId(eRpcId);
		header.setServerIp(host.getIp());
		header.setServiceName(serviceName);
		header.setVersion(version);
		request.setHeader(header);
		request.setBody(serializer.reqBodySerialize(params));
		
		ERpcFuture future = FutureContainer.createFuture(10000L,eRpcId);
		Client.getClient().sendRequest(host.getIp(), host.getPort(), serializer.reqSerialize(request));
		ERpcResponse response =  future.get();
		return serializer.respBodyDeSerialize(response.getBody(), classes);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		// TODO Auto-generated method stub
		providerCache = ProviderCacheFactory.getProdiderCache();
		initDigister();
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		// TODO Auto-generated method stub
		this.applicationContext = applicationContext;
	}
	
	private void initDigister() throws ERpcSerializeException, InstantiationException, IllegalAccessException, ClassNotFoundException {
		String serializerClass = PropertiesUtil.getConfig("serializer");
		if(serializerClass == null || "".equals(serializerClass.trim())) {
			throw new ERpcSerializeException("serializer not found!");
		}
		
		this.serializer = (ERpcSerialize) this.getClass().getClassLoader().loadClass(serializerClass).newInstance();
	}

	public ERpcSerialize getSerializer() {
		return serializer;
	}
}