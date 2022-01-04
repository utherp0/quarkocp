package org.uth.quarkube;

import io.fabric8.kubernetes.api.model.ListOptionsBuilder;
import io.fabric8.kubernetes.api.model.Node;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.kubernetes.client.informers.SharedInformerFactory;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.annotations.QuarkusMain;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Objects;

public class ControllerTest implements QuarkusApplication
{
  @Inject
  KubernetesClient client;

  @Inject
  SharedInformerFactory sharedInformerFactory;

  @Inject
  ResourceEventHandler<Node> nodeEventHandler;

  @Override
  public int run( String... args ) throws Exception
  {
    try
    {
      client.nodes().list( new ListOptionsBuilder().withLimit(1L).build());
    }
    catch( KubernetesClientException exc )
    {
      System.out.println( "Exception thrown " + exc.toString());
      return 1;
    }

    sharedInformerFactory.startAllRegisteredInformers().get();

    final var nodeHandler = sharedInformerFactory.getExistingSharedIndexInformer( Node.class );
    nodeHandler.addEventHandler( nodeEventHandler );

    Quarkus.waitForExit();
    return 0;
  }

  void onShutDown(@Observes ShutdownEvent event )
  {
    sharedInformerFactory.stopAllRegisteredInformers(true);
  }

  public static void main( String... args )
  {
    System.out.println( "In ControllerTest main...");
    Quarkus.run(ControllerTest.class, args );
  }

  @ApplicationScoped
  static final class ControllerTestConfig
  {
    @Inject
    KubernetesClient client;
  }

  @Singleton
  SharedInformerFactory sharedInformerFactory()
  {
    return client.informers();
  }

  @Singleton
  SharedIndexInformer<Node> nodeInformer( SharedInformerFactory factory )
  {
    return factory.sharedIndexInformerFor(Node.class, 0);
  }

  @Singleton
  SharedIndexInformer<Pod> podInformer( SharedInformerFactory factory )
  {
    return factory.sharedIndexInformerFor(Pod.class, 0 );
  }

  @Singleton
  ResourceEventHandler<Node> nodeReconciler( SharedIndexInformer<Node> nodeInformer, SharedIndexInformer<Pod> podInformer )
  {
    return new ResourceEventHandler<>()
    {
      @Override
      public void onAdd( Node node )
      {
        System.out.printf( "node: %s%n", Objects.requireNonNull( node.getMetadata()).getName());
        podInformer.getIndexer().list().stream()
                .map( pod -> Objects.requireNonNull(pod.getMetadata()).getName())
                .forEach(podName -> System.out.printf("pod name: %s%n", podName ));
      }

      @Override
      public void onUpdate(Node odlObj, Node newObj ){}

      @Override
      public void onDelete(Node node, boolean deletedFinalStateUnknown) {}
    };
  }
}
