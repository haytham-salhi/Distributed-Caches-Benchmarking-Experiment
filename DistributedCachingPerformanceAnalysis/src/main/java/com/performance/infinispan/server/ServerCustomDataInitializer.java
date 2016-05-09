package com.performance.infinispan.server;

import java.io.IOException;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.marshall.ProtoStreamMarshaller;
import org.infinispan.protostream.DescriptorParserException;
import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.MessageMarshaller;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.annotations.ProtoSchemaBuilder;
import org.infinispan.query.remote.client.ProtobufMetadataManagerConstants;

import com.performance.GeneralArguments;
import com.performance.model.Employee;
import com.performance.model.Organization;
import com.performance.model.dataAPI;

public class ServerCustomDataInitializer {
	
	private static String serverIP = GeneralArguments.serverIP;
	
	public static void main(String[] args) throws DescriptorParserException, IOException {
		ConfigurationBuilder cb = new ConfigurationBuilder();
		
		
		cb.addServers(serverIP + ":11222;" + serverIP + ":11322;" + serverIP + ":11422;" + serverIP + ":11522").marshaller(new ProtoStreamMarshaller());
		
		///API entry point, by default it connects to localhost:11222
		RemoteCacheManager rcm = new RemoteCacheManager(cb.build());
		
		
		SerializationContext srcCtx = ProtoStreamMarshaller.getSerializationContext(rcm);
		
		srcCtx.registerProtoFiles(FileDescriptorSource.fromResources("/com/performance/infinispan/proto/library.proto"));
		srcCtx.registerMarshaller(new EmployeeMarshaller());
		srcCtx.registerMarshaller(new OrganizationMarshaller());
		
	    ProtoSchemaBuilder protoSchemaBuilder = new ProtoSchemaBuilder();
	    String memoSchemaFile = protoSchemaBuilder
                .fileName("library.proto")
                .packageName("com.performance.infinispan.proto")
                .addClass(Employee.class)
                .addClass(Organization.class)
                .build(srcCtx);
	    
	    RemoteCache<String, String> metadataCache = rcm.getCache(ProtobufMetadataManagerConstants.PROTOBUF_METADATA_CACHE_NAME);
        metadataCache.put("library.proto", memoSchemaFile);
		
		//obtain a handle to the remote default cache
		RemoteCache<Integer, Employee> cache = rcm.getCache();
		
        if(cache.isEmpty()) {
            dataAPI dataApi = new dataAPI();
            
            for (int i = 0; i < GeneralArguments.cacheSize; i++) {
            	cache.put(i, dataApi.getEmployee(i));
    		}	
            
            System.out.println(cache.get(3));
            System.out.println("Initialization done!");
        }
		
		rcm.stop();
	}
	
	private static class EmployeeMarshaller implements MessageMarshaller<Employee> {
		public EmployeeMarshaller() {
		}

		@Override
		public Class<? extends Employee> getJavaClass() {
			// TODO Auto-generated method stub
			return Employee.class;
		}

		@Override
		public String getTypeName() {
			// TODO Auto-generated method stub
			return "com.performance.model.Employee";
		}

		@Override
		public Employee readFrom(
				org.infinispan.protostream.MessageMarshaller.ProtoStreamReader reader)
				throws IOException {
			int id = reader.readInt("ID");
			String name = reader.readString("name");
			int age = reader.readInt("age");
			String password = reader.readString("password");
			Organization organization = reader.readObject("organization", Organization.class);
			
			return new Employee(id, name, age, password, organization);
		}

		@Override
		public void writeTo(
				org.infinispan.protostream.MessageMarshaller.ProtoStreamWriter writer,
				Employee t) throws IOException {
			writer.writeInt("ID", t.getID());
			writer.writeString("name", t.getName());
			writer.writeInt("age", t.getAge());
			writer.writeString("password", t.getPassword());
			writer.writeObject("organization", t.getOrganization(), Organization.class);
		}
	}
	
	private static class OrganizationMarshaller implements MessageMarshaller<Organization> {
		public OrganizationMarshaller() {
		}

		@Override
		public Class<? extends Organization> getJavaClass() {
			// TODO Auto-generated method stub
			return Organization.class;
		}

		@Override
		public String getTypeName() {
			// TODO Auto-generated method stub
			return "com.performance.model.Organization";
		}

		@Override
		public Organization readFrom(
				org.infinispan.protostream.MessageMarshaller.ProtoStreamReader reader)
				throws IOException {
			int id = reader.readInt("ID");
			String name = reader.readString("name");
			String acronym = reader.readString("acronym");
			String employeeNumber = reader.readString("employeeNumber");
			
			return new Organization(id, name, acronym, employeeNumber);
		}

		@Override
		public void writeTo(
				org.infinispan.protostream.MessageMarshaller.ProtoStreamWriter writer,
				Organization t) throws IOException {
			writer.writeInt("ID", t.getID());
			writer.writeString("name", t.getName());
			writer.writeString("acronym", t.getAcronym());
			writer.writeString("employeeNumber", t.getEmployeeNumber());
		}
	}
	

}
