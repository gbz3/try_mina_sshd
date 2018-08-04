package com.github.gbz3.try_mina_sshd;

import java.io.ByteArrayOutputStream;
import java.security.KeyPair;
import java.util.Arrays;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ClientChannel;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.config.keys.ClientIdentityLoader;
import org.apache.sshd.client.future.ConnectFuture;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.FactoryManager;
import org.apache.sshd.common.config.keys.FilePasswordProvider;
import org.apache.sshd.common.io.AbstractIoServiceFactoryFactory;
import org.apache.sshd.common.io.IoServiceFactory;
import org.apache.sshd.common.io.nio2.Nio2ServiceFactory;
import org.apache.sshd.common.util.threads.ThreadUtils;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args ) throws Exception
    {
        System.out.println( "Hello World!" );
        
        try ( SshClient client = SshClient.setUpDefaultClient() ) {
        	client.setIoServiceFactoryFactory( new AbstractIoServiceFactoryFactory( ThreadUtils.newFixedThreadPool( "hoge", 2 ), false ) {

				@Override
				public IoServiceFactory create( FactoryManager manager ) {
					return new Nio2ServiceFactory( manager, getExecutorService(), isShutdownOnExit() );
				}
        		
        	});
    		final KeyPair kp = ClientIdentityLoader.DEFAULT.loadClientIdentity( System.getenv( "env.key" ), FilePasswordProvider.EMPTY );
        	client.addPublicKeyIdentity( kp );
        	client.start();
        	
        	ConnectFuture cf = client.connect( System.getenv( "env.user" ), System.getenv( "env.host" ), Integer.parseInt( System.getenv( "env.port" ) ) );
        	cf.await( 2000L );
//        	cf.verify( 2000L );
        	
        	try ( ClientSession session = cf.getSession() ) {
        		session.auth().verify( 2000L );
        		
        		final ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        		final ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        		try ( ClientChannel shell = session.createExecChannel( "sleep 5; hostname; echo $$" ) ) {
           		//try ( ChannelShell shell = session.createShellChannel() ) {
           			//shell.setAgentForwarding( false );
//           			shell.setIn( new NoCloseInputStream( new ByteArrayInputStream( "sleep 5; hostname; echo $$".getBytes() ) ) );
//           			shell.setOut( new NoCloseOutputStream( stdout ) );
//           			shell.setErr( new NoCloseOutputStream( stderr ) );
           			//shell.setIn( new ByteArrayInputStream( "sleep 5; hostname; echo $$".getBytes() ) );
           			shell.setOut( stdout );
           			shell.setErr( stderr );
           			shell.open().await();
        			shell.waitFor( Arrays.asList( ClientChannelEvent.CLOSED, ClientChannelEvent.EOF ), 0 );
        			
        			System.out.println( "[" + stdout.toString() + "][" + stderr.toString() + "]" );
        			
        		} finally {
        			session.close();
        		}
        		
        	} finally {
        		client.stop();
        	}
        	
        }
    }
}
