//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// This file is a part of the 'coroutines' project.
// Copyright 2018 Elmar Sonnenschein, esoco GmbH, Flensburg, Germany
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//	  http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
package de.esoco.coroutine;

import org.junit.BeforeClass;
import org.junit.Test;

import static de.esoco.coroutine.ChannelId.stringChannel;
import static de.esoco.coroutine.CoroutineScope.launch;
import static de.esoco.coroutine.Coroutines.EXCEPTION_HANDLER;
import static de.esoco.coroutine.step.ChannelReceive.receive;
import static de.esoco.coroutine.step.Select.select;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/********************************************************************
 * Test of {@link Coroutine}.
 *
 * @author eso
 */
public class SelectTest
{
	//~ Static fields/initializers ---------------------------------------------

	private static final ChannelId<String> CHANNEL_A = stringChannel("A");
	private static final ChannelId<String> CHANNEL_B = stringChannel("B");
	private static final ChannelId<String> CHANNEL_C = stringChannel("C");

	private static final Coroutine<Void, String> SELECT_ABC =
		Coroutine.first(
			select(receive(CHANNEL_A)).or(receive(CHANNEL_B))
			.or(receive(CHANNEL_C)));

	//~ Static methods ---------------------------------------------------------

	/***************************************
	 * Test class setup.
	 */
	@BeforeClass
	public static void setup()
	{
		// suppress stacktraces from error testing
		Coroutines.getDefaultContext().set(EXCEPTION_HANDLER, t ->{});
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * Test of channel select.
	 */
	@Test
	public void testChannelSelect()
	{
		testSelect(CHANNEL_A, true);
		testSelect(CHANNEL_B, true);
		testSelect(CHANNEL_C, true);
		testSelect(CHANNEL_A, false);
		testSelect(CHANNEL_B, false);
		testSelect(CHANNEL_C, false);
	}

	/***************************************
	 * Test selecting a certain channel.
	 *
	 * @param rId    The channel ID
	 * @param bAsync Async or blocking
	 */
	private void testSelect(ChannelId<String> rId, boolean bAsync)
	{
		launch(
			run ->
			{
				Channel<String> channel = run.context().getChannel(rId);

				// send first if blocking or else scope will be blocked...
				if (!bAsync)
				{
					channel.sendBlocking("TEST-" + rId);
				}

				Continuation<String> c =
					bAsync ? run.async(SELECT_ABC) : run.blocking(SELECT_ABC);

				if (bAsync)
				{
					channel.sendBlocking("TEST-" + rId);
				}

				assertEquals("TEST-" + rId, c.getResult());
				assertTrue(c.isFinished());
			});
	}
}
