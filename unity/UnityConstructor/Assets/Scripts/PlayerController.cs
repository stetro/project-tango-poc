using UnityEngine;
using System.Collections;

public class PlayerController : MonoBehaviour
{
	void FixedUpdate ()
	{
		if (Input.touches.Length > 0) {
			foreach (Touch t in Input.touches) {
				float speed = 0.03f;
				float moveVertical = 0.0f;
				float moveHorizontal = 0.0f;
				if (t.position.x < (Screen.width / 2)) {
					if (t.position.y > Screen.height / 2) {
						moveVertical = speed;
					} else {
						moveVertical = -speed;
					}
				} else {
					if (t.position.x > ((Screen.width / 2)+Screen.width/4)) {
						moveHorizontal = speed;
					} else {
						moveHorizontal = -speed;
					}
				}
				Vector3 movement =  Camera.main.transform.TransformVector(new Vector3(moveHorizontal, 0.0f, moveVertical));
				transform.position = new Vector3(transform.position.x+movement.x, transform.position.y+movement.y, transform.position.z+movement.z);
			}
		}
	}
}