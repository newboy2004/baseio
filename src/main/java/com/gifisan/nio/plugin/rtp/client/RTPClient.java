package com.gifisan.nio.plugin.rtp.client;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson.JSONObject;
import com.gifisan.nio.client.ClientSession;
import com.gifisan.nio.client.ClientUDPConnector;
import com.gifisan.nio.common.CloseUtil;
import com.gifisan.nio.component.DatagramPacketAcceptor;
import com.gifisan.nio.component.UDPEndPoint;
import com.gifisan.nio.component.future.ReadFuture;
import com.gifisan.nio.component.protocol.DatagramPacket;
import com.gifisan.nio.plugin.jms.JMSException;
import com.gifisan.nio.plugin.jms.Message;
import com.gifisan.nio.plugin.jms.TextMessage;
import com.gifisan.nio.plugin.jms.client.MessageConsumer;
import com.gifisan.nio.plugin.jms.client.MessageProducer;
import com.gifisan.nio.plugin.jms.client.OnMessage;
import com.gifisan.nio.plugin.jms.client.impl.DefaultMessageConsumer;
import com.gifisan.nio.plugin.jms.client.impl.DefaultMessageProducer;
import com.gifisan.nio.plugin.rtp.RTPException;
import com.gifisan.nio.plugin.rtp.server.RTPCreateRoomServlet;
import com.gifisan.nio.plugin.rtp.server.RTPJoinRoomServlet;
import com.gifisan.nio.plugin.rtp.server.RTPLoginServlet;

public class RTPClient implements Closeable{

	private boolean			logined	= false;
	private ClientSession		session	= null;
	private String				roomID	= null;
	private int				roomIDNo = -1;
	private MessageProducer		producer	= null;
	private MessageConsumer		consumer = null;
	private ClientUDPConnector	connector	= null;
	private UDPReceiveHandle receiveHandle = null;

	public RTPClient(ClientSession session,UDPReceiveHandle handle,String customerID) throws Exception {
		this.connector = new ClientUDPConnector(session);
		this.session = session;
		this.receiveHandle = handle;
		this.producer = new DefaultMessageProducer(session);
		this.consumer = new DefaultMessageConsumer(session, customerID);
	}
	
	public void sendDatagramPacket(DatagramPacket packet) throws RTPException{

		if (roomID == null) {
			throw new RTPException("none roomID,create room first");
		}
		
		connector.sendDatagramPacket(packet);
	}

	public boolean createRoom(String customerID) throws RTPException {

		ReadFuture future;

		try {
			future = session.request(RTPCreateRoomServlet.SERVICE_NAME, null);
		} catch (IOException e) {
			throw new RTPException(e.getMessage(), e);
		}

		String roomID = future.getText();

		if ("-1".equals(roomID)) {
			throw new RTPException("create room failed");
		}

		this.roomID = roomID;
		
		this.roomIDNo = Integer.parseInt(roomID);

		this.inviteCustomer(customerID);

		return true;
	}
	
	public boolean joinRoom(String roomID) throws RTPException {

		ReadFuture future;

		try {
			future = session.request(RTPJoinRoomServlet.SERVICE_NAME, roomID);
		} catch (IOException e) {
			throw new RTPException(e.getMessage(), e);
		}
		
		return "T".equals(future.getText());
	}

	public void inviteCustomer(String customerID) throws RTPException {

		if (roomID == null) {
			throw new RTPException("none roomID,create room first");
		}
		
		String param = "{'cmd':'invite','roomID':'"+roomID+"'}";

		TextMessage message = new TextMessage("msgID", customerID, param);

		try {
			producer.offer(message);
		} catch (JMSException e) {
			throw new RTPException(e);
		}
	}
	
	public void inviteReply(String customerID) throws RTPException {
		
		String param = "{'cmd':'invite-reply'}";
		
		
		TextMessage message = new TextMessage("msgID", customerID, param);

		try {
			producer.offer(message);
		} catch (JMSException e) {
			throw new RTPException(e);
		}
	}

	public void login(String username, String password) throws RTPException {
		if (logined) {
			return;
		}

		Map<String, Object> param = new HashMap<String, Object>();
		param.put("username", username);
		param.put("password", password);
		String paramString = JSONObject.toJSONString(param);

		ReadFuture future;
		try {
			future = session.request(RTPLoginServlet.SERVICE_NAME, paramString);
		} catch (IOException e) {
			throw new RTPException(e.getMessage(), e);
		}
		String result = future.getText();
		boolean logined = "T".equals(result);
		if (!logined) {
			throw new RTPException("用户名密码错误！");
		}
		
		
		try {
			this.consumer.receive(new OnMessage() {
				
				public void onReceive(Message message) {
					receiveHandle.onMessage(RTPClient.this,message);
				}
			});
		} catch (JMSException e) {
			throw new RTPException(e);
		}
		
		connector.onDatagramPacketReceived(new DatagramPacketAcceptor() {
			
			public void accept(UDPEndPoint endPoint, DatagramPacket packet) throws IOException {
				receiveHandle.onReceiveUDPPacket(RTPClient.this,packet);
			}
		});
	}
	
	public void close() throws IOException {
		CloseUtil.close(connector);
	}

	public void logout() {
		this.logined = false;
	}

	public int getRoomIDNo() {
		return roomIDNo;
	}
	
	public void setRoomID(String roomID){
		this.roomID = roomID;
		this.roomIDNo = Integer.valueOf(roomID);
	}
}
