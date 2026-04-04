import { useEffect, useRef } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import toast from 'react-hot-toast';
import { useAuth } from '../context/AuthContext';

export function useWebSocket() {
  const { user } = useAuth();
  const clientRef = useRef(null);

  useEffect(() => {
    if (!user?.id) return;

    const client = new Client({
      webSocketFactory: () => new SockJS('http://localhost:8081/ws'),
      onConnect: () => {
        client.subscribe(`/topic/notifications/${user.id}`, (msg) => {
          const data = JSON.parse(msg.body);
          if (data.type === 'CLAIM_APPROVED') {
            toast.success(`💰 ${data.message}`, { duration: 5000 });
          } else {
            toast(data.message);
          }
        });
      },
      reconnectDelay: 5000,
    });

    client.activate();
    clientRef.current = client;

    return () => client.deactivate();
  }, [user?.id]);
}
