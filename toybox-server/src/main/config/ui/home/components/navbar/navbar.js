module.exports = {
    data: function() {
        return  {
          componentName: 'Navigation Bar',
          // Dummy user
          user:{
              username: 'test',
              name: 'test_name',
              lastname: 'test_lastname',
              avatarUrl: 'http://via.placeholder.com/250x250'
          },
          defaultNotification:{
              avatarUrl: '../../images/users/system.png',
              message: 'There are no new unread notifications',
              id:'0',
              date: 'Now',
          },
          // Dummy notifications
          notifications:[
              {
                  avatarUrl: 'http://via.placeholder.com/250x250',
                  message: 'Eu sale officiis per. Ad laoreet civibus eum, partem docendi tincidunt ad mei. Cu maiorum oportere salutandi nam. Qui ne harum labitur nostrud, scripta salutatus per ut. Detracto signiferumque vis eu.',
                  id:'1',
                  date: '1 hour ago',
              },
              {
                  avatarUrl: 'http://via.placeholder.com/250x250',
                  message: 'Eu sale officiis per. Ad laoreet civibus eum, partem docendi tincidunt ad mei. Cu maiorum oportere salutandi nam. Qui ne harum labitur nostrud, scripta salutatus per ut. Detracto signiferumque vis eu.',
                  id:'2',
                  date: '2 hours ago',
              },
              {
                  avatarUrl: 'http://via.placeholder.com/250x250',
                  message: 'Eu sale officiis per. Ad laoreet civibus eum, partem docendi tincidunt ad mei. Cu maiorum oportere salutandi nam. Qui ne harum labitur nostrud, scripta salutatus per ut. Detracto signiferumque vis eu.',
                  id:'3',
                  date: '2 minutes ago',
              },
              {
                  avatarUrl: 'http://via.placeholder.com/250x250',
                  message: 'Eu sale officiis per. Ad laoreet civibus eum, partem docendi tincidunt ad mei. Cu maiorum oportere salutandi nam. Qui ne harum labitur nostrud, scripta salutatus per ut. Detracto signiferumque vis eu.',
                  id:'4',
                  date: '2 minutes ago',
              },
              {
                  avatarUrl: 'http://via.placeholder.com/250x250',
                  message: 'Eu sale officiis per. Ad laoreet civibus eum, partem docendi tincidunt ad mei. Cu maiorum oportere salutandi nam. Qui ne harum labitur nostrud, scripta salutatus per ut. Detracto signiferumque vis eu.',
                  id:'5',
                  date: '2 minutes ago',
              }
          ]
        }
    },
    components:{
        'notification' : httpVueLoader('../notification/notification.vue')
    }
}