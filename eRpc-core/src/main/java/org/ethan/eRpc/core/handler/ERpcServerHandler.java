package org.ethan.eRpc.core.handler;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.ethan.eRpc.core.context.SpringContextHolder;
import org.ethan.eRpc.core.filter.ERpcFilter;
import org.ethan.eRpc.core.request.ERpcRequest;
import org.ethan.eRpc.core.route.ERpcRequestRouter;
import org.ethan.eRpc.core.route.RouteIndicator;
import org.ethan.eRpc.core.serialize.ERpcSerialize;
import org.ethan.eRpc.core.serialize.ERpcSerializeException;
import org.ethan.eRpc.core.util.PropertiesUtil;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelHandler.Sharable;

@Component
@Sharable
public class ERpcServerHandler extends ChannelInboundHandlerAdapter implements InitializingBean{
	
	@Autowired
	private SpringContextHolder springContextHolder;
	
	@Autowired
	private ERpcRequestRouter requestRouter;
	
	private List<ERpcFilter>filters;
	
	private ApplicationContext springContext;
	
	private ERpcSerialize serializer;
	
	private RouteIndicator routeIndicator;
	
	
	
	public ERpcServerHandler() {}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		// TODO Auto-generated method stub
		 System.out.println(ctx.channel().localAddress().toString() + " Chanel Activated!");
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		// TODO Auto-generated method stub
		System.out.println(ctx.channel().localAddress().toString() + " Channel deactivated!");
	}

	/**
	 * 服务器收到客户端请求后触发的方法
	 */
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		// TODO Auto-generated method stub
		//1.读取请求,解码
		Object request = this.requestDecorder.decord(msg);
		
		ERpcRequest socketRequest = new ERpcRequest(msg,request);
		
		SocketResponse socketResponse = new SocketResponse(ctx.channel(), "UTF-8");
		
		
		//2.组装请求上下文
		ERpcRequestContext requestContext = new ERpcRequestContext(ctx,socketRequest,socketResponse);
		
		//3.前置过滤器
		if(this.filters != null && !this.filters.isEmpty()) {
        	ERpcFilterChain chain = new ERpcFilterChain(this.filters);
            chain.doPreFilter(chain, requestContext);
        }
		
		//4.将请求路由至对应Controller,获取响应
		ERpcRequestRouter.ControllerMethod cm = requestRouter.getControllerMethod(this.routeIndicator.getRouteIndicator(socketRequest.getRequestMessage()));
		if(cm == null) {
			ctx.channel().writeAndFlush("No mapping found").sync();
			return;
		}
		Object response = cm.getMethod().invoke(cm.getControllerBean(), socketRequest.getRequestMessage());
		
		//5.后置过滤器
		socketResponse.setResponseMsg(response);
		
		ERpcRequestContext responseContext = new ERpcRequestContext(ctx, socketRequest, socketResponse);
		if(this.filters != null && !this.filters.isEmpty()) {
        	ERpcFilterChain chain = new ERpcFilterChain(this.filters);
            chain.doAfterFilter(chain, responseContext);
        }
		
		//6.响应编码
		byte[] responseBytes = this.responseEncorder.encord(response);
        
        //3.生成响应，发送至客户端
        ctx.channel().writeAndFlush(responseBytes).sync();
       
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		// TODO Auto-generated method stub
		System.out.println("Data receive complete!");
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		// TODO Auto-generated method stub
		ctx.close();
	    System.out.println("Error occurs when handle request:" + cause.getMessage());
	}
	
	private void getSpringContext() {
		springContext = springContextHolder.getSpringApplicationContext();
	}
	
	private void initFilterChain() {
        Map<String,ERpcFilter>filterMaps = springContext.getBeansOfType(ERpcFilter.class);
        if(filters != null && !filters.isEmpty()) {
        	this.filters = new ArrayList<ERpcFilter>();
        	for(Map.Entry<String,ERpcFilter>ent : filterMaps.entrySet()) {
        		this.filters.add(ent.getValue());
        	}
        	
        	//对拦截器排序
        	filters.sort(new Comparator<ERpcFilter>() {
				@Override
				public int compare(ERpcFilter o1, ERpcFilter o2) {
					// TODO Auto-generated method stub
					return o1.getOrder() - o2.getOrder();
				}
			});
        	System.out.println("==============Filter chain begin================");
        	for(ERpcFilter f : filters) {
        		System.out.println(f.getOrder()+":"+f.getClass().getName());
        	}
        	System.out.println("==============Filter chain end================");
        }else {
        	System.out.println("No filter found");
        }
	}
	
	private void initDigister() {
		String serializerClass = PropertiesUtil.getConfig("serializer");
		if(serializerClass == null || "".equals(serializerClass.trim())) {
			throw new ERpcSerializeException("serializer not found!");
		}
		
		this.serializer = (ERpcSerialize) this.getClass().getClassLoader().loadClass(serializerClass).newInstance();
		
		System.out.println("Use ["+serializerClass+"] as requestDecorder");
	}
	
	private void initRouteIndicator() {
		Map<String,RouteIndicator>routeIndicators = springContext.getBeansOfType(RouteIndicator.class);
		if(routeIndicators.size() == 1) {
			routeIndicator = (RouteIndicator)routeIndicators.values().toArray()[0];
		}else {
			for(Map.Entry<String, RouteIndicator>ent : routeIndicators.entrySet()) {
				if(!ent.getKey().equals("defaultIndicator")) {
					routeIndicator = ent.getValue();
					
					break;
				}
			}
		}
		System.out.println("Use ["+routeIndicator.getClass().getName()+"] as routeIndicator");
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		// TODO Auto-generated method stub
		getSpringContext();
		
		initFilterChain();
		
		initDigister();
		
		initRouteIndicator();
	}
}
