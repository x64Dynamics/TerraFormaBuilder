"""RCON transport must handle fragmented TCP reads and authenticate before stopping."""
import socket
import struct
import unittest
from unittest.mock import patch
from runtime import receive, response, stop_server


class FragmentedConnection:
    def __init__(self, incoming):
        self.incoming = incoming
        self.sent = []

    def recv(self, count):
        chunk = self.incoming[:min(count, 2)]
        self.incoming = self.incoming[len(chunk):]
        return chunk

    def sendall(self, payload):
        self.sent.append(payload)

    def __enter__(self):
        return self

    def __exit__(self, *args):
        pass


class RuntimeTest(unittest.TestCase):
    def test_fragmented_packet(self):
        payload = struct.pack('<iii', 10, 1, 2) + b'\0\0'
        self.assertEqual((1,2), response(FragmentedConnection(payload)))

    def test_closed_connection_is_reported(self):
        with self.assertRaises(ConnectionError):
            receive(FragmentedConnection(b''), 4)

    def test_failed_authentication_never_sends_stop(self):
        connection = FragmentedConnection(struct.pack('<iii', 10, -1, 2) + b'\0\0')
        with patch.object(socket, 'create_connection', return_value=connection):
            with self.assertRaises(ValueError):
                stop_server(12345, 'test-secret')
        self.assertEqual(1, len(connection.sent))
        self.assertNotIn(b'stop', connection.sent[0])


if __name__ == '__main__':
    unittest.main()
