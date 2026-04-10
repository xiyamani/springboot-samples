import { createRouter, createWebHistory } from 'vue-router'
import SpecialListView from '../views/SpecialListView.vue'
import SpecialDetailView from '../views/SpecialDetailView.vue'

export default createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/special' },
    {
      path: '/special',
      name: 'special-list',
      component: SpecialListView,
    },
    {
      path: '/special/:id',
      name: 'special-detail',
      component: SpecialDetailView,
    },
  ],
})
