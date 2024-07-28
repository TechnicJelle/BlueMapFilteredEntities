{
	const { PoiMarker, animate, EasingFunctions } = window.BlueMap;
	const originalUpdateFromData = PoiMarker.prototype.updateFromData;
	PoiMarker.prototype.updateFromData = function (markerData) {
		if (!(this.data?.id ?? "").startsWith("bmfe.") || (!this.position.x && !this.position.y && !this.position.z)) {
			return originalUpdateFromData.call(this, markerData)
		}
		let startPos = {
			x: this.position.x,
			y: this.position.y,
			z: this.position.z
		}
		originalUpdateFromData.call(this, {
			...markerData,
			position: startPos
		})
		let pos = markerData.position || {}
		let deltaPos = {
			x: (pos.x || 0) - startPos.x,
			y: (pos.y || 0) - startPos.y,
			z: (pos.z || 0) - startPos.z,
		}
		if (deltaPos.x || deltaPos.y || deltaPos.z) {
			animate(progress => {
				let ease = EasingFunctions.easeInOutCubic(progress);
				this.position.setX(startPos.x + deltaPos.x * ease || 0)
				this.position.setY(startPos.y + deltaPos.y * ease || 0)
				this.position.setZ(startPos.z + deltaPos.z * ease || 0)
			}, 1000)
		}
	}
}
