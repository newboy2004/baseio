/*
 * Copyright 2015 GenerallyCloud.com
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */ 
package com.generallycloud.nio.container.rtp.server;

import com.generallycloud.nio.codec.protobase.future.ProtobaseReadFuture;
import com.generallycloud.nio.common.ByteUtil;
import com.generallycloud.nio.component.SocketSession;

public class RTPLeaveRoomServlet extends RTPServlet {

	public static final String	SERVICE_NAME	= RTPLeaveRoomServlet.class.getSimpleName();

	@Override
	public void doAccept(SocketSession session, ProtobaseReadFuture future, RTPSessionAttachment attachment) throws Exception {

		RTPRoom room = attachment.getRtpRoom();
		
		if (room != null) {
//			room.leave(session.getDatagramChannel()); //FIXME udp 
		}

		future.write(ByteUtil.TRUE);
		
		session.flush(future);
	}
}
