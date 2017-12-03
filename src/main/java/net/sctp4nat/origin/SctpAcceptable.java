/*
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.sctp4nat.origin;

import java.io.IOException;

/**
 * This class is used to shield {@link SctpSocket} from the user.
 * 
 * @author Jonas Wagner
 *
 */
public interface SctpAcceptable {

	/**
	 * This method provides the user the possiblity to accept incoming connection
	 * attempts, once a {@link SctpNotification} is called.
	 * 
	 * @return true if association was accepted
	 * @throws IOException
	 */
	boolean acceptNative() throws IOException;
}
