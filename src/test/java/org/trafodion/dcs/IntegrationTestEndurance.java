/**
 *(C) Copyright 2013 Hewlett-Packard Development Company, L.P.
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
package org.trafodion.dcs;

import java.io.IOException;
import java.util.*;
import java.sql.*;
import java.io.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import java.util.concurrent.*;

import org.apache.commons.io.IOUtils;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import org.junit.experimental.categories.Category;

import org.apache.hadoop.conf.Configuration;
import org.trafodion.dcs.Constants;
import org.trafodion.dcs.util.DcsConfiguration;

@Category(IntegrationTests.class)
public class IntegrationTestEndurance {

	private class IntegrationTestEnduranceWorker implements Callable<Integer> {
		private int numWorker;
		private int loops;
		private int delay;
		private int elapsed;
		private String catalog;
		private String schema;
		private String url;
		private String user;
		private String password;
		private long startTotal;
		private long start;
		private long end;
		private Long microseconds;
		private Integer average = 0;

		public IntegrationTestEnduranceWorker(int numWorker,int loops,int delay,int elapsed,String catalog,String schema,String url,String user,String password){
			this.numWorker = numWorker;
			this.loops = loops;
			this.delay = delay;
			this.elapsed = elapsed * 1000; //in milliseconds
			this.catalog = catalog;
			this.schema = schema;
			this.url = url;
			this.user = user;
			this.password = password;
		}

		@Override
		public Integer call() {

			System.out.println("Thread number: " + numWorker);
			//------------------------------------------------------------------------
			// Load JDBC driver
			//
			try {
				Class.forName("com.hp.jdbc.HPT4Driver");
			} catch (ClassNotFoundException e) {
				fail("Thread number: " + numWorker + ". Could not find the JDBC driver class." + e);
				return average;
			}

			// Create property object to hold username & password
			System.out.println("url=" + this.url + ",user=" + this.user + ",password=" + this.password);
			Properties myProp = new Properties();
			myProp.put("user", this.user);
			myProp.put("password", this.password);
			Connection conn;
			String jdbcUrl = this.url;

			startTotal = System.currentTimeMillis();

			int i = 1;
			while (true) {
				start = System.nanoTime();

				try {
					conn = DriverManager.getConnection(jdbcUrl, myProp);
				} catch (SQLException e) {
					fail("Thread number/loop: " + numWorker + "/" + i + ". Could not connect to database." + e);
					return average;
				}

				System.out.println("Thread number/loop: " + numWorker + "/" + i + ". Connection open.");

				if (delay != 0){
					System.out.println("Thread number: " + numWorker + " sleeps: " + delay + " miliseconds");
					try {
						Thread.sleep(delay);
					} catch (Exception e) {	}
				}				
				
				try {
					conn.close();
				} catch (SQLException e){
					System.out.println("Thread number/loop: " + numWorker + "/" + i + ".Close connection error." + e);
					return average;
				}

				end = System.nanoTime();
				microseconds = (end - start) / 1000;
				if (average == 0)
					average = microseconds.intValue();
				else
					average = (average + microseconds.intValue())/2;

				System.out.println("Thread number/loop: " + numWorker + "/" + i + ". Connection closed. Elapsed time (microseconds): " + microseconds);

				if (elapsed == 0){
					if (i >= loops ) break;
				} else {
					if (System.currentTimeMillis() >= startTotal + elapsed ) break;
				}
				i++;
			}

			return average;
		}
	}
	
	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testEndurance() throws Exception {
		Configuration conf = DcsConfiguration.create();
		int numWorkers = conf.getInt("integration.test.endurance.workers",1);
		int loops = conf.getInt("integration.test.endurance.loops",1);
		int delay = conf.getInt("integration.test.endurance.delay",0);
		int elapsed = conf.getInt("integration.test.endurance.elapsed",0);
		String catalog = conf.get("hpt4jdbc.properties.catalog","");
		String schema = conf.get("hpt4jdbc.properties.schema","");
		String url = conf.get("hpt4jdbc.properties.url","");
		String user = conf.get("hpt4jdbc.properties.user","");
		String password = conf.get("hpt4jdbc.properties.password","");
		
		ExecutorService tpes = Executors.newCachedThreadPool();
		IntegrationTestEnduranceWorker workers[] = new IntegrationTestEnduranceWorker[numWorkers];
		Future futures[] = new Future[numWorkers];

		for (int i = 0; i < numWorkers; i++) {
			workers[i] = new IntegrationTestEnduranceWorker(i,loops,delay,elapsed,catalog,schema,url,user,password);
			futures[i]=tpes.submit(workers[i]);
		}
		
		for (int i = 0; i < numWorkers; i++) {
			try {
				System.out.println("Ending thread: " + (i + 1) + ", average elapsed time: " + futures[i].get());
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println(e.getMessage());
			}
		}
		
		System.out.println("All threads ended");
	}
}