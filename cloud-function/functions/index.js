const {onDocumentCreated} = require("firebase-functions/v2/firestore");
const admin = require('firebase-admin');
admin.initializeApp();

exports.onPhotoUpload = onDocumentCreated("photos/{photoId}", async (event) => {
    try {
        // Get the photo data
        const photoData = event.data.data();
        const authorId = photoData.authorId;
        const authorName = photoData.uploadedBy;
        const photoDescription = photoData.description;

        // Validate required data
        if (!authorId || !authorName) {
            console.error('Missing required photo data:', { authorId, authorName });
            return null;
        }

        // Get the author's document to get followers array
        const authorDoc = await admin.firestore()
            .collection('users')
            .doc(authorId)
            .get();

        if (!authorDoc.exists) {
            console.error('Author document not found:', authorId);
            return null;
        }

        const authorData = authorDoc.data();
        console.log('Author data:', authorData);
        const followers = authorData.followers || [];
        console.log('Followers array:', followers);

        if (followers.length === 0) {
            console.log('No followers found for user:', authorId);
            return null;
        }

        // Get all follower data in a single query
        const followersData = await admin.firestore()
            .collection('users')
            .where(admin.firestore.FieldPath.documentId(), 'in', followers)
            .get();

        // Extract FCM tokens
        const followerTokens = followersData.docs
            .map(doc => {
                const data = doc.data();
                console.log('Follower data:', data);
                return data.fcmToken;
            })
            .filter(token => token);

        console.log('Valid FCM tokens:', followerTokens);

        if (followerTokens.length === 0) {
            console.log('No valid FCM tokens found for followers');
            return null;
        }

        // Send notifications one by one instead of using multicast
        for (const token of followerTokens) {
            try {
                const message = {
                    token: token,
                    notification: {
                        title: `${authorName} posted a new photo`,
                        body: photoDescription || 'Check out their latest upload!'
                    },
                    data: {
                        photoId: event.params.photoId,
                        authorId: authorId,
                        type: 'new_photo',
                        click_action: 'FLUTTER_NOTIFICATION_CLICK',
                        // Add these fields for deep linking
                        screen: 'profile',
                        userId: authorId
                    },
                    android: {
                        priority: 'high',
                        notification: {
                            sound: 'default',
                            clickAction: 'FLUTTER_NOTIFICATION_CLICK'
                        }
                    }
                };

                const response = await admin.messaging().send(message);
                console.log('Successfully sent message to token:', token, response);
            } catch (error) {
                console.error('Error sending message to token:', token, error);
            }
        }
        
        return null;
    } catch (error) {
        console.error('Error in onPhotoUpload function:', error);
        return null;
    }
});