package org.ethan.eRpc.common.serialize;


import java.util.List;
import java.util.Map;

import org.ethan.eRpc.common.bean.ERpcRequest;
import org.ethan.eRpc.common.bean.ERpcResponse;
import org.ethan.eRpc.common.bean.ServiceBean;
import org.ethan.eRpc.common.exception.ERpcSerializeException;

import io.netty.buffer.ByteBuf;

public interface ERpcSerialize{
	/**
	 *  请求序列化
	 * @param obj
	 * @return
	 */
	public byte[] reqSerialize(ERpcRequest request) throws ERpcSerializeException;
	
	/**
	 *  请求参数反序列化
	 * @param obj
	 * @return
	 */
	public Object[] reqBodyDeSerialize(List<ServiceBean.Param> params , String body) throws ERpcSerializeException;
	
	/**
	 *  请求参数序列化
	 * @param obj
	 * @return
	 */
	public String reqBodySerialize(Map<String,Object> params) throws ERpcSerializeException;
	
	/**
	 *  请求反序列化
	 * @param obj
	 * @return
	 */
	public ERpcRequest reqDeSerialize(ByteBuf stream) throws ERpcSerializeException;
	
	/**
	 *  响应序列化
	 * @param obj
	 * @return
	 */
	public byte[] respSerialize(ERpcResponse response) throws ERpcSerializeException;
	
	/**
	 *  响应体序列化
	 * @param obj
	 * @return
	 */
	public String respBodySerialize(Object result) throws ERpcSerializeException;
	
	/**
	 * 响应体反序列化
	 * @param result
	 * @param classes
	 * @return
	 * @throws ERpcSerializeException
	 */
	public<T> T respBodyDeSerialize(String result,Class<T>classes) throws ERpcSerializeException;
	
	/**
	 *  响应反序列化
	 * @param obj
	 * @return
	 */
	public ERpcResponse respDeSerialize(byte[] stream) throws ERpcSerializeException;
}
